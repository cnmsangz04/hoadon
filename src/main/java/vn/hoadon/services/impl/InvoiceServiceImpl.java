package vn.hoadon.services.impl;

import vn.hoadon.invoice.template.service.InvoiceTemplateBindingService;

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

    @Autowired
    private InvoiceTemplateBindingService templateBindings;

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

    @Override
    public Page<InvoiceDTO> search(Long companyId, Integer category, String q, Short status, java.time.LocalDate date, Pageable pageable) {
        Page<InvoiceEntity> page = invoiceRepository.searchByCategory(companyId, category, q, status, date, pageable);
        return mapToDto(page);
    }

    @Override
    public Page<InvoiceDTO> search(Long companyId, Integer category, Integer stockIssueType, String q, Short status, java.time.LocalDate date, Pageable pageable) {
        Page<InvoiceEntity> page = invoiceRepository.searchByCategoryAndStockIssueType(companyId, category, stockIssueType, q, status, date, pageable);
        return mapToDto(page);
    }

    private Page<InvoiceDTO> mapToDto(Page<InvoiceEntity> page) {
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
            dto.stockIssueType = inv.getStockIssueType();
            Long formId = inv.getFormId() != null ? inv.getFormId().longValue() : null;
            if (formId != null) {
                FormInvoiceEntity form = formCache.computeIfAbsent(formId, id -> formInvoiceRepository.findById(id).orElse(null));
                if (form != null && Objects.equals(form.getCompanyId(), inv.getCompanyId() != null ? inv.getCompanyId().longValue() : null)) {
                    form = InvoiceTemplateBindingService.frozenForm(inv, form);
                    dto.formCode = form.getFormCode();
                    dto.serial = form.getSerial();
                    dto.category = form.getCategory();
                }
            }
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
                            refForm = InvoiceTemplateBindingService.frozenForm(ref, refForm);
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
        } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
            return "";
        }
        return "";
    }

    @Override
    @Transactional
    public InvoiceEntity create(InvoicePayload payload, Long companyId, Long userId) {
        validateFormForCompany(payload != null ? payload.formId : null, companyId, payload != null ? payload.stockIssueType : null);
        InvoiceEntity original = validateRelatedInvoiceForCreate(payload, companyId);
        InvoiceEntity e = new InvoiceEntity();
        applyPayload(e, payload, companyId, userId, true);
        assignInvoiceNumberId(e, companyId, payload);
        e.setIdAttr(generateUniqueIdAttr());
        e.setLookupCode(generateUniqueLookupCode());
        e.setCreatedAt(java.time.LocalDateTime.now());
        e.setUpdatedAt(java.time.LocalDateTime.now());
        templateBindings.pin(e);
        InvoiceEntity saved = invoiceRepository.save(e);
        updateOriginalInvoiceStatus(original, saved.getInvoiceType());
        return saved;
    }

    @Override
    @Transactional
    public InvoiceEntity update(Long id, InvoicePayload payload, Long companyId, Long userId) {
        InvoiceEntity e = invoiceRepository.findById(id).orElse(null);
        if (e == null) return null;
        if (!sameCompany(e.getCompanyId(), companyId)) {
            throw new IllegalArgumentException("Hóa đơn không thuộc công ty hiện tại");
        }
        if (e.getStatus() != null && e.getStatus() != 0) throw new IllegalArgumentException("Chỉ được chỉnh sửa hóa đơn nháp");
        Integer previousFormId = e.getFormId();
        validateFormForCompany(payload != null ? payload.formId : null, companyId, payload != null ? payload.stockIssueType : null);
        validateRelatedInvoiceForCreate(payload, companyId);
        applyPayload(e, payload, companyId, userId, false);
        if (!Objects.equals(previousFormId, e.getFormId())) {
            e.setIssueProfileSnapshotJson(null);
        }
        templateBindings.pin(e);
        assignInvoiceNumberId(e, companyId, payload);
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
    @Transactional
    public InvoiceEntity clone(Long sourceId, Long companyId, Long userId) {
        InvoiceEntity src = invoiceRepository.findById(sourceId).orElse(null);
        if (src == null) return null;
        if (!sameCompany(src.getCompanyId(), companyId)) return null;
        InvoiceEntity e = new InvoiceEntity();
        e.setCompanyId(companyId != null ? companyId.intValue() : src.getCompanyId());
        e.setUserId(userId != null ? userId.intValue() : src.getUserId());
        e.setFormId(src.getFormId());
        e.setStockIssueType(src.getStockIssueType());
        FormInvoiceEntity sourceForm = src.getFormId() == null
                ? null
                : formInvoiceRepository.findById(src.getFormId().longValue()).orElse(null);
        e.setName(invoiceTitle(sourceForm, src.getName()));
        e.setNo(null);
        e.setDateExport(src.getDateExport());
        e.setPaymentType(src.getPaymentType());
        e.setPayment(src.getPayment());
        e.setStatus((short)0);
        e.setCurrency(src.getCurrency());
        e.setExchangeRate(src.getExchangeRate());
        e.setTotal(src.getTotal());
        e.setVatAmount(src.getVatAmount());
        e.setAmount(src.getAmount());
        e.setAmountInWords(src.getAmountInWords());
        boolean noTax = sourceForm != null && (Integer.valueOf(2).equals(sourceForm.getCategory()) || Integer.valueOf(6).equals(sourceForm.getCategory()));
        e.setVatAmount(noTax ? 0d : src.getVatAmount());
        e.setAmount(noTax ? (src.getAmount() != null ? src.getAmount() : src.getTotal()) : src.getAmount());
        e.setVatRate(noTax ? null : src.getVatRate());
        e.setVatRateOther(noTax ? 0 : src.getVatRateOther());
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
        e.setCodeCqt(null);
        e.setLookupCode(generateUniqueLookupCode());
        e.setIdAttr(generateUniqueIdAttr());
        InvoicePayload numberingPayload = new InvoicePayload();
        numberingPayload.formId = src.getFormId() != null ? src.getFormId().longValue() : null;
        numberingPayload.stockIssueType = src.getStockIssueType();
        Long targetCompanyId = companyId != null
                ? companyId
                : src.getCompanyId() != null ? src.getCompanyId().longValue() : null;
        assignInvoiceNumberId(e, targetCompanyId, numberingPayload);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        templateBindings.pin(e);
        return invoiceRepository.save(e);
    }

    private void assignInvoiceNumberId(InvoiceEntity e, Long companyId, InvoicePayload p) {
        if (e == null) return;
        Long formId = (p != null && p.formId != null) ? p.formId : (e.getFormId() != null ? e.getFormId().longValue() : null);
        if (companyId == null || formId == null) return;
        java.util.List<vn.hoadon.entity.InvoiceNumberEntity> lst = invoiceNumberRepository.findByCompanyIdAndFormId(companyId, formId);
        if (lst != null && !lst.isEmpty()) {
            vn.hoadon.entity.InvoiceNumberEntity selected = lst.stream().filter(x -> java.util.Objects.equals(x.getStatus(), 1)).findFirst().orElse(lst.get(0));
            if (selected != null && selected.getId() != null) {
                e.setInvoiceNumberId(selected.getId().intValue());
            }
            return;
        }
        FormInvoiceEntity form = formInvoiceRepository.findById(formId).orElse(null);
        if (form == null || form.getCategory() == null) return;
        vn.hoadon.entity.InvoiceNumberEntity counter = new vn.hoadon.entity.InvoiceNumberEntity();
        counter.setCompanyId(companyId);
        counter.setFormId(formId);
        counter.setCategory(form.getCategory());
        counter.setTotal(0);
        counter.setStatus(1);
        counter.setCreatedAt(java.time.LocalDateTime.now());
        counter.setUpdatedAt(java.time.LocalDateTime.now());
        vn.hoadon.entity.InvoiceNumberEntity saved = invoiceNumberRepository.save(counter);
        if (saved.getId() != null) {
            e.setInvoiceNumberId(saved.getId().intValue());
        }
    }

    private void validateFormForCompany(Long formId, Long companyId, Integer stockIssueType) {
        if (formId == null) return;
        FormInvoiceEntity form = formInvoiceRepository.findById(formId).orElse(null);
        if (form == null) {
            throw new IllegalArgumentException("Không tìm thấy mẫu hóa đơn");
        }
        if (!Integer.valueOf(0).equals(form.getSystem()) || companyId == null || !companyId.equals(form.getCompanyId())) {
            throw new IllegalArgumentException("Mẫu hóa đơn không thuộc công ty hiện tại");
        }
        if (form.getCategory() == null || (form.getCategory() != 1 && form.getCategory() != 2 && form.getCategory() != 6)) {
            throw new IllegalArgumentException("Loại mẫu hóa đơn không hợp lệ");
        }
        boolean validTaxType = Integer.valueOf(2).equals(form.getCategory())
                ? Integer.valueOf(-1).equals(form.getType())
                : Integer.valueOf(6).equals(form.getCategory())
                ? Integer.valueOf(0).equals(form.getType())
                : form.getType() != null && (Integer.valueOf(1).equals(form.getType()) || Integer.valueOf(2).equals(form.getType()));
        if (!validTaxType) {
            throw new IllegalArgumentException("Loại thuế suất của mẫu hóa đơn không hợp lệ");
        }
        if (Integer.valueOf(6).equals(form.getCategory()) && (stockIssueType == null || !java.util.Objects.equals(stockIssueType, form.getStockIssueType()))) {
            throw new IllegalArgumentException("Loại phiếu xuất kho không khớp mẫu");
        }
    }

    private boolean sameCompany(Integer entityCompanyId, Long actorCompanyId) {
        if (entityCompanyId == null || actorCompanyId == null) return false;
        return actorCompanyId.equals(entityCompanyId.longValue());
    }

    private String generateUniqueIdAttr() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String s = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
            if (!invoiceRepository.existsByIdAttr(s)) return s;
        }
        String s = (Long.toHexString(System.nanoTime()) + java.util.UUID.randomUUID().toString().replace("-", "")).toUpperCase();
        if (s.length() > 32) s = s.substring(0, 32);
        return s;
    }

    private String generateUniqueLookupCode() {
        final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.util.concurrent.ThreadLocalRandom rnd = java.util.concurrent.ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 20; attempt++) {
            String s = randomString(rnd, ALPHANUM, 10);
            if (!invoiceRepository.existsByLookupCode(s)) return s;
        }
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
        if (companyId != null) e.setCompanyId(companyId.intValue());
        if (userId != null) e.setUserId(userId.intValue());
        if (p.formId != null) e.setFormId(p.formId.intValue());
        Long selectedFormId = p.formId != null ? p.formId : (e.getFormId() == null ? null : e.getFormId().longValue());
        FormInvoiceEntity selectedForm = selectedFormId == null
                ? null
                : formInvoiceRepository.findById(selectedFormId).orElse(null);
        boolean salesInvoice = selectedForm != null && Integer.valueOf(2).equals(selectedForm.getCategory());
        boolean stockIssue = selectedForm != null && Integer.valueOf(6).equals(selectedForm.getCategory());
        if (stockIssue) validateStockIssuePayload(p.stockInfo, selectedForm.getStockIssueType());
        e.setStockIssueType(stockIssue ? selectedForm.getStockIssueType() : null);
        e.setName(invoiceTitle(selectedForm, e.getName()));
        e.setNo(p.no);
        e.setDateExport(p.dateExport);
        e.setPaymentType(p.paymentType != null ? p.paymentType : 1);
        e.setPayment((short)1);
        e.setStatus(p.status != null ? p.status : (short)0);
        e.setCurrency(p.currency != null ? p.currency : "VND");
        e.setExchangeRate(p.exchangeRate != null ? p.exchangeRate : 1d);
        e.setTotal(p.total != null ? p.total : 0d);
        e.setVatAmount(salesInvoice || stockIssue ? 0d : (p.vatAmount != null ? p.vatAmount : 0d));
        e.setAmount(salesInvoice || stockIssue
                ? (p.amount != null ? p.amount : (p.total != null ? p.total : 0d))
                : (p.amount != null ? p.amount : 0d));
        e.setAmountInWords(p.amountInWords);
        e.setVatRate(salesInvoice || stockIssue ? null : (p.vatRate != null ? p.vatRate : (short)-1));
        e.setVatRateOther(salesInvoice || stockIssue ? 0 : (p.vatRateOther != null ? p.vatRateOther : 0));
        e.setBill(null);
        e.setOthers(stockIssue ? serializeSafely(p.stockInfo) : null);
        e.setRelated(normalizeRelatedReason(p.relatedReason));
        e.setCustomer(serializeSafely(stockIssue ? customerFromStockInfo(p.customer, p.stockInfo) : p.customer));
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

    private String invoiceTitle(FormInvoiceEntity form, String currentName) {
        if (form != null && form.getName() != null && !form.getName().isBlank()) return form.getName().trim();
        if (currentName != null && !currentName.isBlank()) return currentName;
        return form != null && Integer.valueOf(2).equals(form.getCategory())
                ? "Hóa đơn bán hàng"
                : form != null && Integer.valueOf(6).equals(form.getCategory())
                ? (Integer.valueOf(1).equals(form.getStockIssueType()) ? "Phiếu xuất kho kiêm vận chuyển nội bộ" : "Phiếu xuất kho hàng gửi bán đại lý")
                : "Hóa đơn giá trị gia tăng";
    }

    private void applyInvoiceRelationFields(InvoiceEntity e, InvoicePayload p, boolean isCreate) {
        boolean hasRelation = p.referenceId != null || p.invoiceType != null || p.invoiceTypeAdjust != null || p.relatedReason != null;
        if (!isCreate && !hasRelation) return;

        Short invoiceType = p.invoiceType != null ? p.invoiceType : (short)0;
        Short invoiceTypeAdjust = p.invoiceTypeAdjust != null ? p.invoiceTypeAdjust : (short)0;
        e.setReferenceId(p.referenceId != null ? p.referenceId.intValue() : null);
        e.setInvoiceType(invoiceType);
        e.setInvoiceTypeAdjust(invoiceType == 2 ? invoiceTypeAdjust : (short)0);
        e.setRelated(invoiceType == 1 || invoiceType == 2 ? normalizeRelatedReason(p.relatedReason) : null);
    }

    private String normalizeRelatedReason(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String normalized = value.trim();
        if (normalized.matches("\\d{1,2}")) {
            int code = Integer.parseInt(normalized);
            if (code >= 1 && code <= 14) return String.valueOf(code);
        }
        throw new IllegalArgumentException("Lý do điều chỉnh/thay thế không hợp lệ");
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
        if (p.relatedReason == null || p.relatedReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn lý do điều chỉnh/thay thế");
        }
        normalizeRelatedReason(p.relatedReason);
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

    private Object customerFromStockInfo(Object customer, Object stockInfo) {
        if (customer != null || !(stockInfo instanceof Map<?, ?> info)) return customer;
        Map<String, Object> result = new HashMap<>();
        result.put("name", firstNonBlank(info, "buyerName", "buyer_name", "name"));
        result.put("buyer", firstNonBlank(info, "receiverName", "receiver_name", "buyer"));
        result.put("taxcode", firstNonBlank(info, "buyerTaxCode", "buyer_tax_code", "taxcode"));
        result.put("address", firstNonBlank(info, "buyerAddress", "buyer_address", "address"));
        return result;
    }

    private void validateStockIssuePayload(Object stockInfo, Integer stockIssueType) {
        if (!(stockInfo instanceof Map<?, ?> info)) throw new IllegalArgumentException("Thiếu thông tin phiếu xuất kho");
        if (isBlank(firstNonBlank(info, "buyerAddress", "buyer_address", "address"))) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ kho nhận hàng");
        }
        if (isBlank(firstNonBlank(info, "vehicle", "vehicleNumber", "vehicle_number"))) {
            throw new IllegalArgumentException("Vui lòng nhập phương tiện vận chuyển");
        }
        if (Integer.valueOf(1).equals(stockIssueType)) {
            if (isBlank(firstNonBlank(info, "dispatchOrderNo", "dispatch_order_no", "lddnbo"))) {
                throw new IllegalArgumentException("Vui lòng nhập lệnh điều động nội bộ");
            }
        } else if (Integer.valueOf(2).equals(stockIssueType)) {
            if (isBlank(firstNonBlank(info, "transportName", "transport_name"))) {
                throw new IllegalArgumentException("Vui lòng nhập người vận chuyển");
            }
            if (isBlank(firstNonBlank(info, "economicContractNo", "economic_contract_no"))
                    || isBlank(firstNonBlank(info, "economicContractDate", "economic_contract_date"))) {
                throw new IllegalArgumentException("Vui lòng nhập số và ngày hợp đồng kinh tế");
            }
        }
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    private String firstNonBlank(Map<?, ?> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value != null && !String.valueOf(value).trim().isEmpty()) return String.valueOf(value).trim();
        }
        return "";
    }
}
