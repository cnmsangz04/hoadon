package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
    public Page<InvoiceDTO> search(String q, Short status, Pageable pageable) {
        Page<InvoiceEntity> page = invoiceRepository.search(q, status, pageable);
        return mapToDto(page);
    }

    @Override
    public Page<InvoiceDTO> search(String q, Short status, java.time.LocalDate date, Pageable pageable) {
        Page<InvoiceEntity> page = invoiceRepository.search(q, status, date, pageable);
        return mapToDto(page);
    }

    private Page<InvoiceDTO> mapToDto(Page<InvoiceEntity> page) {
        // Preload related forms and users to avoid N+1 lookups
        Map<Long, FormInvoiceEntity> formCache = new HashMap<>();
        Map<Long, UserEntity> userCache = new HashMap<>();

        return page.map(inv -> {
            InvoiceDTO dto = new InvoiceDTO();
            dto.id = inv.getId();
            dto.no = inv.getNo();
            dto.dateExport = inv.getDateExport();
            dto.lookupCode = inv.getLookupCode();
            dto.codeCqt = inv.getCodeCqt();
            dto.amount = inv.getAmount();
            dto.status = inv.getStatus();
            Long formId = inv.getFormId() != null ? inv.getFormId().longValue() : null;
            if (formId != null) {
                FormInvoiceEntity form = formCache.computeIfAbsent(formId, id -> formInvoiceRepository.findById(id).orElse(null));
                if (form != null) {
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
    public InvoiceEntity create(InvoicePayload payload, Long companyId, Long userId) {
        InvoiceEntity e = new InvoiceEntity();
        applyPayload(e, payload, companyId, userId, true);
        // Ensure invoice_number_id from formId
        assignInvoiceNumberId(e, companyId, payload);
        // Generate unique id_attr (32 uppercase hex) and lookup_code (10 uppercase alphanumeric)
        e.setIdAttr(generateUniqueIdAttr());
        e.setLookupCode(generateUniqueLookupCode());
        e.setCreatedAt(java.time.LocalDateTime.now());
        e.setUpdatedAt(java.time.LocalDateTime.now());
        return invoiceRepository.save(e);
    }

    @Override
    public InvoiceEntity update(Long id, InvoicePayload payload, Long companyId, Long userId) {
        InvoiceEntity e = invoiceRepository.findById(id).orElse(null);
        if (e == null) return null;
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
        e.setInvoiceType(src.getInvoiceType());
        e.setInvoiceTypeAdjust(src.getInvoiceTypeAdjust());
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

    private String generateUniqueIdAttr() {
        // 32-character uppercase hex derived from UUID without hyphens
        for (int attempt = 0; attempt < 10; attempt++) {
            String s = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase(); // 32 chars
            if (!invoiceRepository.existsByIdAttr(s)) return s;
        }
        // Fallback: nanoTime + random, truncated to 32, uppercase
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
        // Fallback with timestamp prefix trimmed to 10
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
        if (isCreate) {
            e.setSendMailDirectly((short)0);
            e.setDiscount(0d);
            e.setDiscountAmount(0d);
            e.setVatAmountDiscount(0d);
            e.setTypeApi((short)0);
            e.setInvoiceType((short)0);
            e.setInvoiceTypeAdjust((short)0);
            e.setStatusConvert((short)0);
        }
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
