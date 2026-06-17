package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.dto.InvoiceDTO;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.InvoiceNumberRepository;
import vn.hoadon.repositories.InvoiceRepository;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.services.InvoiceService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private FormInvoiceRepository formInvoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvoiceNumberRepository invoiceNumberRepository;

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

    @Override
    public Page<InvoiceDTO> search(Long companyId, String q, Short status, Pageable pageable) {
        Page<InvoiceEntity> page = invoiceRepository.search(companyId, q, status, pageable);
        return mapToDto(page);
    }

    @Override
    public Page<InvoiceDTO> search(Long companyId, String q, Short status, java.time.LocalDate date, Pageable pageable) {
        Page<InvoiceEntity> page = invoiceRepository.search(companyId, q, status, date, pageable);
        return mapToDto(page);
    }

    private Page<InvoiceDTO> mapToDto(Page<InvoiceEntity> page) {
        // Preload related forms and users to avoid N+1 lookups
        Map<Long, FormInvoiceEntity> formCache = new HashMap<>();
        Map<Long, UserEntity> userCache = new HashMap<>();
        Map<Long, InvoiceEntity> invoiceCache = new HashMap<>();

        return page.map(inv -> {
            InvoiceDTO dto = new InvoiceDTO();
            dto.id = inv.getId();
            dto.no = inv.getNo();
            dto.dateExport = inv.getDateExport();
            dto.lookupCode = inv.getLookupCode();
            dto.codeCqt = inv.getCodeCqt();
            dto.amount = inv.getAmount();
            dto.status = inv.getStatus();
            dto.referenceId = inv.getReferenceId();
            dto.invoiceType = inv.getInvoiceType();
            dto.invoiceTypeAdjust = inv.getInvoiceTypeAdjust();
            Long formId = inv.getFormId() != null ? inv.getFormId().longValue() : null;
            if (formId != null) {
                FormInvoiceEntity form = formCache.computeIfAbsent(formId, id -> formInvoiceRepository.findById(id).orElse(null));
                if (form != null && Objects.equals(form.getCompanyId(), inv.getCompanyId() != null ? inv.getCompanyId().longValue() : null)) {
                    dto.formCode = form.getFormCode();
                    dto.serial = form.getSerial();
                }
            }
            // Populate customerName from invoices.customer JSON
            dto.customerName = extractCustomerName(inv.getCustomer());
            Long userId = inv.getUserId() != null ? inv.getUserId().longValue() : null;
            dto.userId = userId;
            if (userId != null) {
                UserEntity u = userCache.computeIfAbsent(userId, id -> userRepository.findById(id).orElse(null));
                dto.username = (u != null ? u.getUsername() : null);
            }
            Long referenceId = inv.getReferenceId() != null ? inv.getReferenceId().longValue() : null;
            if (referenceId != null) {
                InvoiceEntity ref = invoiceCache.computeIfAbsent(referenceId, id -> invoiceRepository.findById(id).orElse(null));
                if (ref != null && Objects.equals(inv.getCompanyId(), ref.getCompanyId())) {
                    dto.referenceNo = ref.getNo();
                    Long refFormId = ref.getFormId() != null ? ref.getFormId().longValue() : null;
                    if (refFormId != null) {
                        FormInvoiceEntity refForm = formCache.computeIfAbsent(refFormId, id -> formInvoiceRepository.findById(id).orElse(null));
                        if (refForm != null && Objects.equals(refForm.getCompanyId(), inv.getCompanyId() != null ? inv.getCompanyId().longValue() : null)) {
                            dto.referenceFormCode = refForm.getFormCode();
                            dto.referenceSerial = refForm.getSerial();
                        }
                    }
                }
            }
            return dto;
        });
    }

    private String extractCustomerName(String customerJson) {
        if (customerJson == null || customerJson.trim().isEmpty()) return "";
        try {
            String t = customerJson.trim();
            if (t.startsWith("{") && t.endsWith("}")) {
                java.util.Map<?,?> map = JSON.readValue(t, java.util.Map.class);
                Object name = map.get("name");
                Object buyer = map.get("buyer");
                String sName = name != null ? String.valueOf(name).trim() : "";
                String sBuyer = buyer != null ? String.valueOf(buyer).trim() : "";
                if (!sName.isEmpty()) return sName;
                if (!sBuyer.isEmpty()) return sBuyer;
                return "";
            }
        } catch (Exception ignore) {}
        return "";
    }

    @Override
    @Transactional
    public InvoiceEntity create(InvoicePayload payload, Long companyId, Long userId) {
        validateFormForCompany(payload != null ? payload.formId : null, companyId);
        InvoiceEntity original = validateRelatedInvoiceForCreate(payload, companyId);
        InvoiceEntity e = new InvoiceEntity();
        applyPayload(e, payload, companyId, userId, true);
        // Ensure invoice_number_id from formId
        assignInvoiceNumberId(e, companyId, payload);
        // Generate unique id_attr (32 uppercase hex) and lookup_code (10 uppercase alphanumeric)
        e.setIdAttr(generateUniqueIdAttr());
        e.setLookupCode(generateUniqueLookupCode());
        e.setCreatedAt(java.time.LocalDateTime.now());
        e.setUpdatedAt(java.time.LocalDateTime.now());
        InvoiceEntity saved = invoiceRepository.save(e);
        updateOriginalInvoiceStatus(original, saved.getInvoiceType());
        return saved;
    }

    @Override
    public InvoiceEntity update(Long id, InvoicePayload payload, Long companyId, Long userId) {
        InvoiceEntity e = invoiceRepository.findById(id).orElse(null);
        if (e == null) return null;
        if (!sameCompany(e.getCompanyId(), companyId)) {
            throw new IllegalArgumentException("Hóa đơn không thuộc công ty hiện tại");
        }
        validateFormForCompany(payload != null ? payload.formId : null, companyId);
        validateRelatedInvoiceForCreate(payload, companyId);
        applyPayload(e, payload, companyId, userId, false);
        // Re-assign invoice_number_id if formId changed or missing
        assignInvoiceNumberId(e, companyId, payload);
        // Keep existing id_attr/lookup_code; generate if missing
        if (e.getIdAttr() == null || e.getIdAttr().isEmpty()) {
            e.setIdAttr(generateUniqueIdAttr());
        }
        if (e.getLookupCode() == null || e.getLookupCode().isEmpty()) {
            e.setLookupCode(generateUniqueLookupCode());
        }
        e.setUpdatedAt(java.time.LocalDateTime.now());
        return invoiceRepository.save(e);
    }

    @Override
    public InvoiceEntity clone(Long sourceId, Long companyId, Long userId) {
        InvoiceEntity src = invoiceRepository.findById(sourceId).orElse(null);
        if (src == null) return null;
        if (!sameCompany(src.getCompanyId(), companyId)) return null;
        InvoiceEntity e = new InvoiceEntity();
        // Copy fields from source but reset identifiers and status/number
        e.setCompanyId(companyId != null ? companyId.intValue() : src.getCompanyId());
        e.setUserId(userId != null ? userId.intValue() : src.getUserId());
        e.setFormId(src.getFormId());
        e.setName(src.getName());
        e.setNo(null); // new invoice should not reuse number
        e.setDateExport(src.getDateExport());
        e.setPaymentType(src.getPaymentType());
        e.setPayment(src.getPayment());
        e.setStatus((short)0); // reset to draft
        e.setCurrency(src.getCurrency());
        e.setExchangeRate(src.getExchangeRate());
        e.setTotal(src.getTotal());
        e.setVatAmount(src.getVatAmount());
        e.setAmount(src.getAmount());
        e.setAmountInWords(src.getAmountInWords());
        e.setVatRate(src.getVatRate());
        e.setVatRateOther(src.getVatRateOther());
        e.setBill(src.getBill());
        e.setOthers(src.getOthers());
        e.setRelated(src.getRelated());
        e.setCustomer(src.getCustomer());
        e.setDetail(src.getDetail());
        e.setReferenceId(null);
        e.setInvoiceType((short)0);
        e.setInvoiceTypeAdjust((short)0);
        e.setSendMailDirectly((short)0);
        e.setDiscount(src.getDiscount());
        e.setDiscountAmount(src.getDiscountAmount());
        e.setVatAmountDiscount(src.getVatAmountDiscount());
        e.setTypeApi(src.getTypeApi());
        e.setStatusConvert((short)0);
        // Reset code fields
        e.setCodeCqt(null);
        e.setLookupCode(generateUniqueLookupCode());
        e.setIdAttr(generateUniqueIdAttr());
        // Assign invoice number id based on form/company
        assignInvoiceNumberId(e, companyId != null ? companyId : (src.getCompanyId() != null ? src.getCompanyId().longValue() : null), new InvoicePayload(){ { this.formId = src.getFormId() != null ? src.getFormId().longValue() : null; } });
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return invoiceRepository.save(e);
    }

    private void assignInvoiceNumberId(InvoiceEntity e, Long companyId, InvoicePayload p) {
        if (e == null) return;
        Long formId = (p != null && p.formId != null) ? p.formId : (e.getFormId() != null ? e.getFormId().longValue() : null);
        if (companyId == null || formId == null) return;
        java.util.List<vn.hoadon.entity.InvoiceNumberEntity> lst = invoiceNumberRepository.findByCompanyIdAndFormId(companyId, formId);
        if (lst != null && !lst.isEmpty()) {
            // Strategy: pick the first active (status=1), else first item
            vn.hoadon.entity.InvoiceNumberEntity selected = lst.stream().filter(x -> java.util.Objects.equals(x.getStatus(), 1)).findFirst().orElse(lst.get(0));
            if (selected != null && selected.getId() != null) {
                e.setInvoiceNumberId(selected.getId().intValue());
            }
        }
    }

    private void validateFormForCompany(Long formId, Long companyId) {
        if (formId == null) return;
        FormInvoiceEntity form = formInvoiceRepository.findById(formId).orElse(null);
        if (form == null) {
            throw new IllegalArgumentException("Không tìm thấy mẫu hóa đơn");
        }
        if (companyId != null && form.getCompanyId() != null && !companyId.equals(form.getCompanyId())) {
            throw new IllegalArgumentException("Mẫu hóa đơn không thuộc công ty hiện tại");
        }
    }

    private boolean sameCompany(Integer entityCompanyId, Long actorCompanyId) {
        if (entityCompanyId == null || actorCompanyId == null) return false;
        return actorCompanyId.equals(entityCompanyId.longValue());
    }

    private String generateUniqueIdAttr() {
        // 32-character uppercase hex derived from UUID without hyphens
        for (int attempt = 0; attempt < 10; attempt++) {
            String s = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase(); // 32 chars
            if (!invoiceRepository.existsByIdAttr(s)) return s;
        }
        // Dự phòng: nanoTime + random, cắt còn 32 ký tự và viết hoa
        String s = (Long.toHexString(System.nanoTime()) + java.util.UUID.randomUUID().toString().replace("-", "")).toUpperCase();
        if (s.length() > 32) s = s.substring(0, 32);
        return s;
    }

    private String generateUniqueLookupCode() {
        // 10-character uppercase alphanumeric
        final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.util.concurrent.ThreadLocalRandom rnd = java.util.concurrent.ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 20; attempt++) {
            String s = randomString(rnd, ALPHANUM, 10);
            if (!invoiceRepository.existsByLookupCode(s)) return s;
        }
        // Dự phòng với tiền tố timestamp cắt còn 10 ký tự
        String s = ("LK" + Long.toString(System.currentTimeMillis(), 36).toUpperCase() + randomString(rnd, ALPHANUM, 6)).toUpperCase();
        if (s.length() > 10) s = s.substring(0, 10);
        return s;
    }

    private String randomString(java.util.concurrent.ThreadLocalRandom rnd, String chars, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void applyPayload(InvoiceEntity e, InvoicePayload p, Long companyId, Long userId, boolean isCreate) {
        if (e == null || p == null) return;
        // Core identifiers
        if (companyId != null) e.setCompanyId(companyId.intValue());
        if (userId != null) e.setUserId(userId.intValue());
        if (p.formId != null) e.setFormId(p.formId.intValue());
        // Basic info
        e.setName("Hóa đơn GTGT");
        e.setNo(p.no);
        e.setDateExport(p.dateExport);
        e.setPaymentType(p.paymentType != null ? p.paymentType : 1);
        e.setPayment((short)1);
        e.setStatus(p.status != null ? p.status : (short)0);
        e.setCurrency(p.currency != null ? p.currency : "VND");
        // Default exchange_rate = 1 when not provided
        e.setExchangeRate(p.exchangeRate != null ? p.exchangeRate : 1d);
        // Totals
        e.setTotal(p.total != null ? p.total : 0d);
        e.setVatAmount(p.vatAmount != null ? p.vatAmount : 0d);
        e.setAmount(p.amount != null ? p.amount : 0d);
        e.setAmountInWords(p.amountInWords);
        e.setVatRate(p.vatRate != null ? p.vatRate : (short)-1);
        e.setVatRateOther(p.vatRateOther != null ? p.vatRateOther : 0);
        // JSON blobs
        e.setBill(null);
        e.setOthers(null);
        e.setRelated(null);
        e.setCustomer(serializeSafely(p.customer));
        e.setDetail(serializeSafely(p.detail));
        applyInvoiceRelationFields(e, p, isCreate);
        if (isCreate) {
            e.setSendMailDirectly((short)0);
            e.setDiscount(0d);
            e.setDiscountAmount(0d);
            e.setVatAmountDiscount(0d);
            e.setTypeApi((short)0);
            e.setStatusConvert((short)0);
        }
    }

    private void applyInvoiceRelationFields(InvoiceEntity e, InvoicePayload p, boolean isCreate) {
        boolean hasRelation = p.referenceId != null || p.invoiceType != null || p.invoiceTypeAdjust != null;
        if (!isCreate && !hasRelation) return;

        Short invoiceType = p.invoiceType != null ? p.invoiceType : (short)0;
        Short invoiceTypeAdjust = p.invoiceTypeAdjust != null ? p.invoiceTypeAdjust : (short)0;
        e.setReferenceId(p.referenceId != null ? p.referenceId.intValue() : null);
        e.setInvoiceType(invoiceType);
        e.setInvoiceTypeAdjust(invoiceType == 2 ? invoiceTypeAdjust : (short)0);
    }

    private InvoiceEntity validateRelatedInvoiceForCreate(InvoicePayload p, Long companyId) {
        if (p == null) return null;
        short invoiceType = p.invoiceType != null ? p.invoiceType : 0;
        if (invoiceType == 0) return null;
        if (invoiceType != 1 && invoiceType != 2) {
            throw new IllegalArgumentException("Loại hóa đơn xử lý không hợp lệ");
        }
        if (p.referenceId == null) {
            throw new IllegalArgumentException("Thiếu hóa đơn gốc cần xử lý");
        }
        InvoiceEntity original = invoiceRepository.findById(p.referenceId).orElse(null);
        if (original == null) {
            throw new IllegalArgumentException("Không tìm thấy hóa đơn gốc");
        }
        if (companyId != null && original.getCompanyId() != null && !companyId.equals(original.getCompanyId().longValue())) {
            throw new IllegalArgumentException("Hóa đơn gốc không thuộc công ty hiện tại");
        }
        short originalStatus = original.getStatus() != null ? original.getStatus() : 0;
        short originalType = original.getInvoiceType() != null ? original.getInvoiceType() : 0;
        if (invoiceType == 1) {
            if (originalStatus != 3 || (originalType != 0 && originalType != 1)) {
                throw new IllegalArgumentException("Chỉ được thay thế hóa đơn đã phát hành và là hóa đơn gốc hoặc hóa đơn thay thế");
            }
        } else {
            short adjustType = p.invoiceTypeAdjust != null ? p.invoiceTypeAdjust : 0;
            if (adjustType < 1 || adjustType > 3) {
                throw new IllegalArgumentException("Vui lòng chọn loại điều chỉnh hóa đơn");
            }
            if ((originalStatus != 3 && originalStatus != 5) || originalType != 0) {
                throw new IllegalArgumentException("Chỉ được điều chỉnh hóa đơn gốc đã phát hành hoặc đã bị điều chỉnh");
            }
        }
        return original;
    }

    private void updateOriginalInvoiceStatus(InvoiceEntity original, Short invoiceType) {
        if (original == null || invoiceType == null) return;
        if (invoiceType == 1) {
            original.setStatus((short)4);
        } else if (invoiceType == 2) {
            original.setStatus((short)5);
        } else {
            return;
        }
        original.setUpdatedAt(LocalDateTime.now());
        invoiceRepository.save(original);
    }

    private String serializeSafely(Object o) {
        if (o == null) return null;
        try {
            return JSON.writeValueAsString(o);
        } catch (Exception ex) {
            return String.valueOf(o);
        }
    }
}
