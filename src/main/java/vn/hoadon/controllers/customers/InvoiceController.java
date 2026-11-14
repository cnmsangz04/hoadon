package vn.hoadon.controllers.customers;

import vn.hoadon.invoice.template.service.InvoiceDocumentBridge;
import vn.hoadon.invoice.template.service.InvoiceDocumentService;
import vn.hoadon.invoice.template.service.InvoiceTemplateBindingService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.InvoiceDTO;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceEntity;
import vn.hoadon.entity.RegisterInvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.CompanyBankEntity;
import vn.hoadon.entity.BuyInvoiceEntity;
import vn.hoadon.entity.HashInvoiceEntity;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.InvoiceRepository;
import vn.hoadon.repositories.RegisterInvoiceRepository;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.CompanyBankRepository;
import vn.hoadon.repositories.InvoiceNumberRepository;
import vn.hoadon.repositories.SignatureVatRepository;
import vn.hoadon.repositories.BuyInvoiceRepository;
import vn.hoadon.repositories.HashInvoiceRepository;
import vn.hoadon.entity.InvoiceNumberEntity;
import vn.hoadon.entity.SignatureVatEntity;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.services.InvoiceService;
import vn.hoadon.services.HistoryService;
import vn.hoadon.services.SignatureAuthoritiesTaxService;
import vn.hoadon.dto.history.HistoryDto;

import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;

import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.hoadon.repositories.HistoryRepository;
import org.springframework.mail.javamail.JavaMailSender;
import vn.hoadon.messaging.MailJobMessage;
import vn.hoadon.services.MailQueueService;
import vn.hoadon.services.patvan.PatvanCallbackService;
import vn.hoadon.services.patvan.PatvanMessageType;
import vn.hoadon.services.patvan.PatvanPostResult;
import vn.hoadon.services.patvan.PatvanTransmissionService;
import vn.hoadon.util.InvoiceXmlTaxValidator;
import vn.hoadon.worker.MailWorker;

@RestController
@RequestMapping("/v1/invoices")
public class InvoiceController extends BaseController {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private InvoiceService invoiceService;
    @Autowired private FormInvoiceRepository formInvoiceRepository;
    @Autowired private RegisterInvoiceRepository registerInvoiceRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private CompanyBankRepository companyBankRepository;
    @Autowired private InvoiceNumberRepository invoiceNumberRepository;
    @Autowired private SignatureVatRepository signatureVatRepository;
    @Autowired private BuyInvoiceRepository buyInvoiceRepository;
    @Autowired private HashInvoiceRepository hashInvoiceRepository;
    @Autowired private HistoryService historyService;
    @Autowired private SignatureAuthoritiesTaxService signatureAuthoritiesTaxService;
    @Autowired private HistoryRepository historyRepository;
    @Autowired(required = false) private JavaMailSender mailSender;
    @Autowired(required = false) private MailQueueService mailQueueService;
    @Autowired(required = false) private MailWorker mailWorker;
    @Autowired private PatvanTransmissionService patvanTransmissionService;
    @Autowired private PatvanCallbackService patvanCallbackService;

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    @Autowired private InvoiceDocumentBridge documentBridge;
    @Autowired private InvoiceDocumentService invoiceDocuments;
    @Autowired private InvoiceTemplateBindingService templateBindings;

    @GetMapping
    public ResponseEntity<?> list(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) Short status,
            // Lọc theo ngày lập (dateExport). Frontend gửi yyyy-MM-dd.
            @RequestParam(name = "date", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate date,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
    	permission("invoice-list");
    	
        // Get companyId from authenticated user
        Long companyId = null;
        if (user != null && user.getCompanyId() != null) {
            companyId = user.getCompanyId();
        }
        
        // Nếu user không có companyId (trường hợp không mong đợi), trả về kết quả rỗng
        if (companyId == null) {
            return ResponseEntity.ok(new PageDTO<>(java.util.List.of(), 1, size, 0L, 1));
        }
        
        int pageIndex = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<InvoiceDTO> result = invoiceService.search(companyId, q, status, date, pageable);
        return ResponseEntity.ok(new PageDTO<>(result.getContent(), result.getNumber() + 1, result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/prepare")
    public ResponseEntity<?> prepare(@AuthenticationPrincipal UserEntity user) {
        permission("invoice-save");

        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Không xác định được công ty/người dùng"));
        }
        Long companyId = user.getCompanyId();
        // 1) Active VAT form
        FormInvoiceEntity activeVatForm = templateBindings.activeForm(companyId, 1);
        if (activeVatForm == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Chưa có mẫu hóa đơn GTGT được kích hoạt"));
        }
        // 2) Accepted register invoice (status=6), pick latest by effective_date (deterministic, limit 1)
        java.util.List<RegisterInvoiceEntity> acceptedList = registerInvoiceRepository.findLatestAccepted(companyId, org.springframework.data.domain.PageRequest.of(0, 1));
        RegisterInvoiceEntity latestAccepted = acceptedList != null && !acceptedList.isEmpty() ? acceptedList.get(0) : null;
        if (latestAccepted == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Chưa có tờ khai được chấp nhận"));
        }
        // 3) Vat category requires invoice_types contain HDGTGT (array of strings)
        if (activeVatForm.getCategory() != null && activeVatForm.getCategory() == 1) {
            java.util.List<String> invoiceTypes = safeList(latestAccepted.getInvoiceTypes());
            if (!containsToken(invoiceTypes, "HDGTGT")) {
                return ResponseEntity.badRequest().body(new ErrorDTO("Tờ khai không đăng ký loại hóa đơn GTGT"));
            }
        }
        // 4) Check invoice_forms CMa/KCMa depending on haveCode (array of strings)
        java.util.List<String> invoiceForms = safeList(latestAccepted.getInvoiceForms());
        Integer haveCode = activeVatForm.getHaveCode();
        boolean requireCMa = haveCode != null && haveCode == 1;
        boolean okForm = requireCMa ? containsToken(invoiceForms, "CMa") : containsToken(invoiceForms, "KCMa");
        if (!okForm) {
            return ResponseEntity.badRequest().body(new ErrorDTO(requireCMa ? "Tờ khai không đăng ký hình thức Cấp mã" : "Tờ khai không đăng ký hình thức Không cấp mã"));
        }
        // OK
        PrepareDTO dto = new PrepareDTO();
        dto.formId = activeVatForm.getId();
        dto.formName = activeVatForm.getName();
        dto.formCode = activeVatForm.getFormCode();
        dto.serial = activeVatForm.getSerial();
        dto.haveCode = activeVatForm.getHaveCode();
        dto.formType = activeVatForm.getType();
        dto.registerId = latestAccepted.getId();
        dto.registerEffectiveDate = latestAccepted.getEffectiveDate() != null ? latestAccepted.getEffectiveDate().toString() : null;
        dto.companyId = user.getCompanyId();
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal UserEntity user, @RequestBody CreateRequest req) {
    	permission("invoice-save");
    	
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Vui lòng xác định công ty/người dùng"));
        }
        Long companyId = user.getCompanyId();
        Long userId = user.getId();
        InvoiceService.InvoicePayload p = toPayload(req);
        vn.hoadon.entity.InvoiceEntity saved;
        try {
            saved = invoiceService.create(p, companyId, userId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorDTO(ex.getMessage()));
        }
        if (saved == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Không thể lưu hóa đơn"));
        }
        return ResponseEntity.ok(new CreateResponse(saved.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @AuthenticationPrincipal UserEntity user, @RequestBody CreateRequest req) {
    	permission("invoice-save");
    	
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Vui lòng xác định công ty/người dùng"));
        }
        Long companyId = user.getCompanyId();
        Long userId = user.getId();
        InvoiceService.InvoicePayload p = toPayload(req);
        vn.hoadon.entity.InvoiceEntity saved;
        try {
            saved = invoiceService.update(id, p, companyId, userId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorDTO(ex.getMessage()));
        }
        if (saved == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Không tìm thấy hóa đơn để cập nhật"));
        }
        return ResponseEntity.ok(new CreateResponse(saved.getId()));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable("id") Long id, @AuthenticationPrincipal UserEntity user) {
        permission("invoice-delete");
        
        if (id == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Thiếu ID hóa đơn"));
        }
        
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(403).body(new ErrorDTO("Không có quyền thực hiện thao tác này"));
        }
        
        Optional<InvoiceEntity> optInv = invoiceRepository.findById(id);
        InvoiceEntity inv = optInv.orElse(null);
        
        if (inv == null) {
            return ResponseEntity.status(404).body(new ErrorDTO("Không tìm thấy hóa đơn"));
        }
        
        // Check company ownership
        Long userCompanyId = user.getCompanyId();
        Long invoiceCompanyId = inv.getCompanyId() != null ? inv.getCompanyId().longValue() : null;
        if (invoiceCompanyId != null && !userCompanyId.equals(invoiceCompanyId)) {
            return ResponseEntity.status(403).body(new ErrorDTO("Không có quyền xóa hóa đơn của công ty khác"));
        }
        
        // Check if invoice has a number (no > 0) - prevent deletion
        Integer no = inv.getNo();
        if (no != null && no > 0) {
            return ResponseEntity.status(400).body(new ErrorDTO("Không thể xóa hóa đơn đã có số"));
        }
        
        // Check status: only allow deletion if status = 0 (Mới khởi tạo)
        Short status = inv.getStatus();
        if (status != null && status != 0) {
            return ResponseEntity.status(400).body(new ErrorDTO("Chỉ có thể xóa hóa đơn ở trạng thái 'Mới khởi tạo'"));
        }
        
        try {
            // Delete invoice
            invoiceRepository.delete(inv);
            
            // Log deletion to history
            try {
                HistoryDto h = new HistoryDto();
                h.setCompanyId(userCompanyId);
                h.setUserId(user.getId());
                h.setTableName("invoices");
                h.setTableId(id);
                h.setTitle("Xóa hóa đơn");
                h.setDescription("Đã xóa hóa đơn ID: " + id);
                h.setShowNotify(0);
                h.setStatus(1);
                h.setType(999); // Custom type for deletion
                historyService.save(h);
            } catch (Exception e) {
                log.warn("Failed to save deletion history for invoice {}: {}", id, e.toString());
            }
            
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("message", "Đã xóa hóa đơn thành công");
                put("id", id);
            }});
        } catch (Exception e) {
            log.error("Failed to delete invoice {}: {}", id, e.toString());
            return ResponseEntity.status(500).body(new ErrorDTO("Xóa hóa đơn thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<?> clone(@PathVariable("id") Long id, @AuthenticationPrincipal UserEntity user) {
        permission("invoice-save");
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Vui lòng xác định công ty/người dùng"));
        }
        Long companyId = user.getCompanyId();
        Long userId = user.getId();
        InvoiceEntity saved = invoiceService.clone(id, companyId, userId);
        if (saved == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Không tìm thấy hóa đơn để sao chép"));
        }
        return ResponseEntity.ok(new CreateResponse(saved.getId()));
    }

    @GetMapping("/processing/lookup")
    public ResponseEntity<?> lookupProcessingInvoice(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(name = "no") Integer no,
            @RequestParam(name = "invoiceType") Short invoiceType
    ) {
        permission("invoice-list");

        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Vui lòng xác định công ty/người dùng"));
        }
        if (no == null || no <= 0) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Vui lòng nhập số hóa đơn cần tìm"));
        }
        if (invoiceType == null || (invoiceType != 1 && invoiceType != 2)) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Loại xử lý hóa đơn không hợp lệ"));
        }

        InvoiceEntity inv = invoiceRepository
                .findFirstByCompanyIdAndNoOrderByIdDesc(user.getCompanyId().intValue(), no)
                .orElse(null);
        if (inv == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Không tìm thấy hóa đơn theo số đã nhập"));
        }

        String validationError = processingValidationError(inv, invoiceType);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(new ErrorDTO(validationError));
        }

        return ResponseEntity.ok(toProcessingInvoiceDto(inv));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id, @AuthenticationPrincipal UserEntity user) {
        if (id == null) return ResponseEntity.badRequest().body(new ErrorDTO("Thiếu ID hóa đơn"));
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(403).body(new ErrorDTO("Không có quyền xem hóa đơn"));
        }
        java.util.Optional<vn.hoadon.entity.InvoiceEntity> opt = invoiceRepository.findById(id);
        vn.hoadon.entity.InvoiceEntity inv = opt.orElse(null);
        if (inv == null) return ResponseEntity.badRequest().body(new ErrorDTO("Không tìm thấy hóa đơn"));
        if (inv.getCompanyId() == null || !user.getCompanyId().equals(inv.getCompanyId().longValue())) {
            return ResponseEntity.status(403).body(new ErrorDTO("Không có quyền xem hóa đơn của công ty khác"));
        }
        // Tạo phản hồi với các khối JSON đã parse
        java.util.Map<String,Object> resp = new java.util.LinkedHashMap<>();
        resp.put("id", inv.getId());
        resp.put("formId", inv.getFormId());
        resp.put("invoiceNumberId", inv.getInvoiceNumberId());
        resp.put("referenceId", inv.getReferenceId());
        resp.put("invoiceType", inv.getInvoiceType());
        resp.put("invoiceTypeAdjust", inv.getInvoiceTypeAdjust());
        resp.put("relatedReason", inv.getRelated());
        resp.put("no", inv.getNo());
        resp.put("dateExport", inv.getDateExport());
        resp.put("paymentType", inv.getPaymentType());
        resp.put("status", inv.getStatus());
        resp.put("orderCode", inv.getLookupCode());
        resp.put("total", inv.getTotal());
        resp.put("vatAmount", inv.getVatAmount());
        resp.put("amount", inv.getAmount());
        resp.put("amountInWords", inv.getAmountInWords());
        resp.put("currency", inv.getCurrency());
        resp.put("exchangeRate", inv.getExchangeRate());
        resp.put("vatRate", inv.getVatRate());
        resp.put("vatRateOther", inv.getVatRateOther());
        if (inv.getFormId() != null) {
            FormInvoiceEntity form = formInvoiceRepository.findById(inv.getFormId().longValue()).orElse(null);
            if (form != null && form.getCompanyId() != null && form.getCompanyId().equals(user.getCompanyId())) {
                resp.put("formName", form.getName());
                resp.put("formCode", form.getFormCode());
                resp.put("serial", form.getSerial());
                resp.put("haveCode", form.getHaveCode());
                resp.put("formType", form.getType());
            }
        }
        // Parse JSON fields
        resp.put("customer", parseJson(inv.getCustomer()));
        resp.put("detail", parseJson(inv.getDetail()));
        return ResponseEntity.ok(resp);
    }

    private Object parseJson(String s) {
        if (s == null || s.isEmpty()) return null;
        String t = s.trim();
        try {
            if (t.startsWith("[") && t.endsWith("]")) {
                return JSON.readValue(t, new TypeReference<java.util.List<java.util.Map<String,Object>>>() {});
            } else if (t.startsWith("{") && t.endsWith("}")) {
                return JSON.readValue(t, new TypeReference<java.util.Map<String,Object>>() {});
            }
        } catch (Exception ignore) {}
        return t;
    }

    private InvoiceService.InvoicePayload toPayload(CreateRequest r) {
        InvoiceService.InvoicePayload p = new InvoiceService.InvoicePayload();
        p.formId = r.formId;
        p.no = r.no;
        p.dateExport = r.dateExport;
        p.paymentType = r.paymentType;
        p.status = r.status;
        p.orderCode = r.orderCode;
        p.customer = r.customer;
        p.detail = r.detail;
        p.total = r.total;
        p.vatAmount = r.vatAmount;
        p.amount = r.amount;
        p.amountInWords = r.amountInWords;
        p.currency = r.currency;
        p.exchangeRate = r.exchangeRate;
        p.vatRate = r.vatRate;
        p.vatRateOther = r.vatRateOther;
        p.referenceId = r.referenceId;
        p.invoiceType = r.invoiceType;
        p.invoiceTypeAdjust = r.invoiceTypeAdjust;
        p.relatedReason = r.relatedReason;
        return p;
    }

    private String processingValidationError(InvoiceEntity inv, Short invoiceType) {
        if (inv == null) return "Không tìm thấy hóa đơn";
        short status = inv.getStatus() != null ? inv.getStatus() : 0;
        short type = inv.getInvoiceType() != null ? inv.getInvoiceType() : 0;
        if (invoiceType == 1) {
            if (status != 3 || (type != 0 && type != 1)) {
                return "Chỉ được thay thế hóa đơn đã phát hành và là hóa đơn gốc hoặc hóa đơn thay thế";
            }
            return null;
        }
        if (invoiceType == 2) {
            if ((status != 3 && status != 5) || type != 0) {
                return "Chỉ được điều chỉnh hóa đơn gốc đã phát hành hoặc đã bị điều chỉnh";
            }
            return null;
        }
        return "Loại xử lý hóa đơn không hợp lệ";
    }

    private java.util.Map<String,Object> toProcessingInvoiceDto(InvoiceEntity inv) {
        java.util.Map<String,Object> dto = new java.util.LinkedHashMap<>();
        dto.put("id", inv.getId());
        dto.put("no", inv.getNo());
        dto.put("dateExport", inv.getDateExport());
        dto.put("status", inv.getStatus());
        dto.put("statusText", statusLabel(inv.getStatus()));
        dto.put("invoiceType", inv.getInvoiceType());
        dto.put("invoiceTypeText", invoiceTypeLabel(inv.getInvoiceType()));
        dto.put("amount", inv.getAmount());
        dto.put("lookupCode", inv.getLookupCode());
        dto.put("customerName", extractCustomerName(inv.getCustomer()));
        if (inv.getFormId() != null) {
            FormInvoiceEntity form = formInvoiceRepository.findById(inv.getFormId().longValue()).orElse(null);
            Long invCompanyId = inv.getCompanyId() != null ? inv.getCompanyId().longValue() : null;
            if (form != null && form.getCompanyId() != null && form.getCompanyId().equals(invCompanyId)) {
                dto.put("formCode", form.getFormCode());
                dto.put("serial", form.getSerial());
            }
        }
        return dto;
    }

    private String extractCustomerName(String customerJson) {
        Object parsed = parseJson(customerJson);
        if (parsed instanceof java.util.Map<?,?> map) {
            String name = invoiceCustomerCompanyName(map);
            String buyer = invoiceCustomerBuyerName(map);
            if (!name.isEmpty()) return name;
            if (!buyer.isEmpty()) return buyer;
        }
        return "";
    }

    static String invoiceCustomerCompanyName(java.util.Map<?, ?> customer) {
        if (customer == null) return "";
        return firstNonBlankText(
                customer.get("name"),
                customer.get("companyName"),
                customer.get("company_name"),
                customer.get("customerName"),
                customer.get("customer_name")
        );
    }

    static String invoiceCustomerBuyerName(java.util.Map<?, ?> customer) {
        if (customer == null) return "";
        return firstNonBlankText(
                customer.get("buyer"),
                customer.get("buyerName"),
                customer.get("buyer_name")
        );
    }

    static String invoiceMailGreetingName(java.util.Map<?, ?> customer) {
        String companyName = invoiceCustomerCompanyName(customer);
        if (!companyName.isEmpty()) return companyName;
        String buyerName = invoiceCustomerBuyerName(customer);
        if (!buyerName.isEmpty()) return buyerName;
        return "Quý khách";
    }

    private static String firstNonBlankText(Object... values) {
        if (values == null) return "";
        for (Object value : values) {
            if (value == null) continue;
            String text = String.valueOf(value).trim();
            if (!text.isEmpty()) return text;
        }
        return "";
    }

    private String statusLabel(Short status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "Mới khởi tạo";
            case 1 -> "Đã ký";
            case 2 -> "Đã gửi thuế";
            case 3 -> "Đã phát hành";
            case 4 -> "Bị thay thế";
            case 5 -> "Bị điều chỉnh";
            case 6 -> "Đã hủy";
            case 7 -> "Không đủ điều kiện";
            default -> "";
        };
    }

    private String invoiceTypeLabel(Short invoiceType) {
        if (invoiceType == null) return "Hóa đơn gốc";
        return switch (invoiceType) {
            case 1 -> "Hóa đơn thay thế";
            case 2 -> "Hóa đơn điều chỉnh";
            default -> "Hóa đơn gốc";
        };
    }

    private static boolean containsToken(java.util.List<String> arr, String token) {
        if (arr == null || token == null) return false;
        for (String s : arr) { if (s != null && s.trim().equalsIgnoreCase(token)) return true; }
        return false;
    }
    private static java.util.List<String> safeList(Object o) {
        if (o == null) return java.util.Collections.emptyList();
        if (o instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<String> list = (java.util.List<String>) o;
            return list;
        }
        String raw = String.valueOf(o);
        if (raw == null || raw.isEmpty()) return java.util.Collections.emptyList();
        String s = raw.trim();
        // Nếu giống mảng JSON thì parse
        if (s.startsWith("[") && s.endsWith("]")) {
            try {
                return JSON.readValue(s, new TypeReference<java.util.List<String>>(){});
            } catch (Exception ignore) { /* fall through to CSV */ }
        }
        // Dự phòng: tách theo CSV
        String[] parts = s.split(",");
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String p : parts) { if (p != null) out.add(p.trim().replaceAll("^\"|\"$", "")); }
        return out;
    }

    public static class PageDTO<T> {
        public java.util.List<T> items;
        public int current_page;
        public int per_page;
        public long total;
        public int last_page;
        public PageDTO(java.util.List<T> items, int currentPage, int perPage, long total, int lastPage) {
            this.items = items; this.current_page = currentPage; this.per_page = perPage; this.total = total; this.last_page = lastPage;
        }
    }
    public static class ErrorDTO { public String message; public ErrorDTO(String m){ this.message = m; } }
    public static class PrepareDTO { public Long formId; public String formName; public String formCode; public String serial; public Integer haveCode; public Integer formType; public Long registerId; public String registerEffectiveDate; public Long companyId; }

    // Incoming request from Vue create form
    public static class CreateRequest {
        public Long formId;
        public Integer no;
        public java.time.LocalDate dateExport;
        public Short paymentType;
        public Short status;
        public String orderCode;
        public Object customer;
        public Object detail;
        public Double total;
        public Double vatAmount;
        public Double amount;
        public String amountInWords;
        public String currency;
        public Double exchangeRate;
        public Short vatRate;
        public Integer vatRateOther;
        public Long referenceId;
        public Short invoiceType;
        public Short invoiceTypeAdjust;
        public String relatedReason;
    }

    public static class CreateResponse { public Long id; public CreateResponse(Long id){ this.id = id; } }
    public static class UsbTokenPrepareRequest { public java.util.List<Long> ids; }

    @GetMapping(value = "/{lookup}/view", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> viewByLookup(@PathVariable("lookup") String lookup,
                                          @RequestParam(name = "taxcode", required = false) String taxcode) {
        if (lookup == null || lookup.trim().isEmpty()) {
            return ResponseEntity.status(400).contentType(MediaType.TEXT_HTML).body("<html><body>Thiếu mã tra cứu</body></html>");
        }
        Optional<InvoiceEntity> optInv = invoiceRepository.findByLookupCode(lookup);
        InvoiceEntity inv = optInv.orElse(null);
        if (inv == null) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_HTML).body("<html><body>Không tìm thấy hóa đơn</body></html>");
        }
        FormInvoiceEntity form = null;
        if (inv.getFormId() != null) {
            Long formIdLong = null;
            Object fid = inv.getFormId();
            if (fid instanceof Number) {
                formIdLong = ((Number) fid).longValue();
            } else {
                try { formIdLong = Long.valueOf(String.valueOf(fid)); } catch (Exception ignore) {}
            }
            if (formIdLong != null) {
                form = formInvoiceRepository.findById(formIdLong).orElse(null);
            }
        }
        CompanyEntity company = resolveInvoiceCompany(inv, form);
        ResponseEntity<?> accessError = validatePublicInvoiceAccess(taxcode, company, MediaType.TEXT_HTML);
        if (accessError != null) return accessError;

        return documentBridge.viewer(documentBridge.invoice(inv.getId(), null), "/v1/invoices/" + lookup + "/download-pdf");
    }

    @GetMapping(value = "/{lookup}/download-xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> downloadXmlByLookup(@PathVariable("lookup") String lookup,
                                                 @RequestParam(name = "taxcode", required = false) String taxcode) {
        Optional<InvoiceEntity> optInv = invoiceRepository.findByLookupCode(lookup);
        InvoiceEntity inv = optInv.orElse(null);
        if (inv == null) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN).body("Không tìm thấy hóa đơn");
        }
        FormInvoiceEntity form = null;
        if (inv.getFormId() != null) {
            Long formIdLong = null;
            Object fid = inv.getFormId();
            if (fid instanceof Number) {
                formIdLong = ((Number) fid).longValue();
            } else {
                try { formIdLong = Long.valueOf(String.valueOf(fid)); } catch (Exception ignore) {}
            }
            if (formIdLong != null) {
                form = formInvoiceRepository.findById(formIdLong).orElse(null);
            }
        }
        CompanyEntity company = resolveInvoiceCompany(inv, form);
        ResponseEntity<?> accessError = validatePublicInvoiceAccess(taxcode, company, MediaType.TEXT_PLAIN);
        if (accessError != null) return accessError;

        CompanyBankEntity bank = resolveCompanyBank(company);
        String xml = getInvoiceXmlByStatus(inv, form, company, bank);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("invoice-" + lookup + ".xml").build());
        return new ResponseEntity<>(xml, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/{lookup}/download-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> downloadPdfByLookup(@PathVariable("lookup") String lookup,
                                                 @RequestParam(name = "taxcode", required = false) String taxcode,
                                                 @RequestParam(required = false) Long artifactId) {
        Optional<InvoiceEntity> optInv = invoiceRepository.findByLookupCode(lookup);
        InvoiceEntity inv = optInv.orElse(null);
        if (inv == null) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN).body("Không tìm thấy hóa đơn");
        }
        FormInvoiceEntity form = null;
        if (inv.getFormId() != null) {
            Long formIdLong = null;
            Object fid = inv.getFormId();
            if (fid instanceof Number) {
                formIdLong = ((Number) fid).longValue();
            } else {
                try { formIdLong = Long.valueOf(String.valueOf(fid)); } catch (Exception ignore) {}
            }
            if (formIdLong != null) {
                form = formInvoiceRepository.findById(formIdLong).orElse(null);
            }
        }
        CompanyEntity company = resolveInvoiceCompany(inv, form);
        ResponseEntity<?> accessError = validatePublicInvoiceAccess(taxcode, company, MediaType.TEXT_PLAIN);
        if (accessError != null) return accessError;

        return documentBridge.pdf(documentBridge.invoice(inv.getId(), artifactId), true, "invoice-" + lookup + ".pdf");
    }

    private void addInvoiceZipAttachmentVars(java.util.Map<String, String> vars, InvoiceEntity inv) throws Exception {
        if (vars == null || inv == null) return;
        byte[] zipBytes = buildInvoiceIssueZip(inv);
        vars.put("ATTACHMENT_ZIP_NAME", buildInvoiceAttachmentZipName(inv));
        vars.put("ATTACHMENT_ZIP_BASE64", java.util.Base64.getEncoder().encodeToString(zipBytes));
    }

    private byte[] buildInvoiceIssueZip(InvoiceEntity inv) throws Exception {
        String baseName = buildInvoiceAttachmentBaseName(inv);
        String xml = buildInvoiceXmlContent(inv);
        byte[] pdfBytes = buildInvoicePdfBytes(inv);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {
            zip.putNextEntry(new java.util.zip.ZipEntry(baseName + ".pdf"));
            zip.write(pdfBytes);
            zip.closeEntry();

            zip.putNextEntry(new java.util.zip.ZipEntry(baseName + ".xml"));
            zip.write(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private String buildInvoiceXmlContent(InvoiceEntity inv) {
        FormInvoiceEntity form = resolveInvoiceForm(inv);
        CompanyEntity company = resolveInvoiceCompany(inv, form);
        CompanyBankEntity bank = resolveCompanyBank(company);
        return getInvoiceXmlByStatus(inv, form, company, bank);
    }

    private byte[] buildInvoicePdfBytes(InvoiceEntity inv) {
        return documentBridge.bytes(documentBridge.invoice(inv.getId(), null));
    }

    private FormInvoiceEntity resolveInvoiceForm(InvoiceEntity inv) {
        if (inv == null || inv.getFormId() == null) return null;
        try {
            return formInvoiceRepository.findById(inv.getFormId().longValue()).orElse(null);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String buildInvoiceAttachmentZipName(InvoiceEntity inv) {
        return buildInvoiceAttachmentBaseName(inv) + ".zip";
    }

    private String buildInvoiceAttachmentBaseName(InvoiceEntity inv) {
        String no = safeStr(inv != null ? inv.getNo() : null);
        String lookup = inv != null && inv.getLookupCode() != null
                ? inv.getLookupCode()
                : safeStr(inv != null ? inv.getId() : null);
        String raw = "hoa-don-" + (!no.isBlank() ? no : lookup);
        String clean = raw.replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("^-+|-+$", "");
        return clean.isBlank() ? "hoa-don" : clean;
    }

    @PostMapping("/sign-token/prepare")
    @Transactional
    public ResponseEntity<?> prepareUsbTokenSign(@RequestBody UsbTokenPrepareRequest req,
                                                 @AuthenticationPrincipal UserEntity user) {
        permission("invoice-save");
        try {
            if (user == null || user.getCompanyId() == null) {
                return ResponseEntity.status(403).body(new ErrorDTO("Không có quyền ký số hóa đơn"));
            }
            java.util.List<Long> ids = normalizeIds(req != null ? req.ids : null);
            if (ids.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorDTO("Vui lòng chọn hóa đơn cần ký"));
            }

            Long companyId = user.getCompanyId();
            companyRepository.lockById(companyId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công ty"));
            java.util.List<InvoiceEntity> invoices = loadInvoicesForUsbToken(ids, companyId);
            int needNumber = 0;
            for (InvoiceEntity inv : invoices) {
                if (inv.getNo() == null || inv.getNo() < 1) needNumber++;
            }
            BuyInvoiceEntity buyInvoice = validateInvoicePackage(companyId, needNumber);
            java.util.Map<Long, FormInvoiceEntity> forms = new java.util.HashMap<>();
            for (InvoiceEntity inv : invoices) {
                forms.put(inv.getId(), requireInvoiceForm(inv, companyId));
            }
            for (InvoiceEntity inv : invoices) {
                FormInvoiceEntity form = forms.get(inv.getId());
                if (inv.getNo() == null || inv.getNo() < 1) {
                    assignInvoiceNo(inv, form, companyId);
                }
                inv.setUpdatedAt(java.time.LocalDateTime.now());
                invoiceRepository.save(inv);
            }

            if (needNumber > 0 && buyInvoice != null) {
                int amountUsed = buyInvoice.getAmountUsed() != null ? buyInvoice.getAmountUsed() : 0;
                buyInvoice.setAmountUsed(amountUsed + needNumber);
                buyInvoice.setUpdatedAt(java.time.LocalDateTime.now());
                buyInvoiceRepository.save(buyInvoice);
            }

            HashInvoiceEntity hash = new HashInvoiceEntity();
            hash.setCompanyId(companyId);
            hash.setUserId(user.getId());
            hash.setNameUser(user.getUsername());
            hash.setHash(createHash(companyId));
            hash.setIds(ids);
            hash.setIdAttr("");
            hash.setSignBy("NBan");
            hash.setType("HDGTGT");
            hashInvoiceRepository.save(hash);

            java.util.Map<String,Object> resp = new java.util.LinkedHashMap<>();
            resp.put("hash", hash.getHash());
            resp.put("signBy", "NBan");
            resp.put("total", ids.size());
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorDTO(ex.getMessage()));
        }
    }

    @GetMapping("/sign-token/status")
    public ResponseEntity<?> usbTokenSignStatus(@RequestParam("hash") String hash,
                                                @RequestParam("ids") java.util.List<Long> requestedIds,
                                                @AuthenticationPrincipal UserEntity user) {
        permission("invoice-save");
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(403).body(new ErrorDTO("Không có quyền kiểm tra trạng thái ký số"));
        }

        java.util.List<Long> ids = normalizeIds(requestedIds);
        if (hash == null || hash.isBlank() || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Thiếu thông tin phiên ký số"));
        }

        int signed = 0;
        for (Long id : ids) {
            InvoiceEntity invoice = invoiceRepository.findById(id).orElse(null);
            if (invoice == null || invoice.getCompanyId() == null
                    || !user.getCompanyId().equals(invoice.getCompanyId().longValue())) {
                return ResponseEntity.status(404).body(new ErrorDTO("Không tìm thấy hóa đơn trong phiên ký"));
            }
            if (invoice.getStatus() != null && invoice.getStatus() == 1) signed++;
        }

        boolean pending = hashInvoiceRepository.findByHashAndCompanyId(hash, user.getCompanyId()).isPresent();
        String state = signed == ids.size() ? "signed" : (pending ? "pending" : "failed");
        java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("state", state);
        response.put("signed", signed);
        response.put("total", ids.size());
        return ResponseEntity.ok(response);
    }

    private java.util.List<Long> normalizeIds(java.util.List<Long> ids) {
        if (ids == null) return java.util.Collections.emptyList();
        java.util.LinkedHashSet<Long> unique = new java.util.LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null && id > 0) unique.add(id);
        }
        return new java.util.ArrayList<>(unique);
    }

    private java.util.List<InvoiceEntity> loadInvoicesForUsbToken(java.util.List<Long> ids, Long companyId) {
        java.util.List<InvoiceEntity> invoices = new java.util.ArrayList<>();
        for (Long invoiceId : ids) {
            InvoiceEntity inv = invoiceRepository.findById(invoiceId).orElse(null);
            if (inv == null) {
                throw new IllegalArgumentException("Không tìm thấy hóa đơn #" + invoiceId);
            }
            Long invCompanyId = inv.getCompanyId() != null ? inv.getCompanyId().longValue() : null;
            if (invCompanyId == null || !companyId.equals(invCompanyId)) {
                throw new IllegalArgumentException("Không có quyền ký hóa đơn #" + invoiceId);
            }
            if (inv.getStatus() != null && inv.getStatus() != 0) {
                throw new IllegalArgumentException("Chỉ được ký hóa đơn mới khởi tạo");
            }
            invoices.add(inv);
        }
        return invoices;
    }

    private FormInvoiceEntity requireInvoiceForm(InvoiceEntity inv, Long companyId) {
        Long formId = inv.getFormId() != null ? inv.getFormId().longValue() : null;
        if (formId == null) {
            throw new IllegalArgumentException("Thiếu mẫu hóa đơn (form_id)");
        }
        FormInvoiceEntity form = formInvoiceRepository.findById(formId).orElse(null);
        if (form == null || form.getCompanyId() == null || !companyId.equals(form.getCompanyId())) {
            throw new IllegalArgumentException("Mẫu hóa đơn không thuộc công ty hiện tại");
        }
        return form;
    }

    private BuyInvoiceEntity validateInvoicePackage(Long companyId, int needNumber) {
        if (needNumber < 1) return null;
        BuyInvoiceEntity buyInvoice = buyInvoiceRepository
                .findFirstByCompanyIdAndStatusOrderByIdDesc(companyId, 1)
                .orElseThrow(() -> new IllegalArgumentException("Công ty chưa có gói hóa đơn được kích hoạt"));
        int amount = buyInvoice.getAmount() != null ? buyInvoice.getAmount() : 0;
        int amountUsed = buyInvoice.getAmountUsed() != null ? buyInvoice.getAmountUsed() : 0;
        if (amountUsed + needNumber > amount) {
            throw new IllegalArgumentException("Quý khách đã sử dụng hết số hóa đơn");
        }
        return buyInvoice;
    }

    private void assignInvoiceNo(InvoiceEntity inv, FormInvoiceEntity form, Long companyId) {
        Long formId = form.getId();
        java.util.List<InvoiceNumberEntity> counters = invoiceNumberRepository.findByCompanyIdAndFormId(companyId, formId);
        InvoiceNumberEntity counter = null;
        for (InvoiceNumberEntity c : counters) {
            if (c.getStatus() != null && c.getStatus() == 1) { counter = c; break; }
        }
        if (counter == null && !counters.isEmpty()) {
            // Pick the first as fallback
            counter = counters.get(0);
        }
        if (counter == null) {
            counter = new InvoiceNumberEntity();
            counter.setCompanyId(companyId);
            counter.setFormId(formId);
            counter.setCategory(1); // VAT
            counter.setTotal(0);
            counter.setStatus(1); // active
            counter.setCreatedAt(java.time.LocalDateTime.now());
            counter = invoiceNumberRepository.save(counter);
        }
        Integer nextNo = (counter.getTotal() != null ? counter.getTotal() : 0) + 1;
        counter.setTotal(nextNo);
        counter.setUpdatedAt(java.time.LocalDateTime.now());
        invoiceNumberRepository.save(counter);

        if (inv.getInvoiceNumberId() == null) {
            inv.setInvoiceNumberId(counter.getId().intValue());
        }
        inv.setNo(nextNo);
    }

    private String createHash(Long companyId) {
        String raw = "PA@Hash:com" + companyId + "," + System.nanoTime() + "," + java.util.UUID.randomUUID();
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }

    // Kiểm tra và chuẩn hóa dữ liệu XML của hóa đơn.
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @PostMapping("/{id}/send-to-cqt")
    @Transactional
    public ResponseEntity<?> sendToCqt(@PathVariable("id") Long id, @AuthenticationPrincipal UserEntity user) {
        permission("invoice-save");
        if (id == null) return ResponseEntity.badRequest().body(new ErrorDTO("Thiếu ID hóa đơn"));
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Không xác định được người dùng/công ty"));
        }
        Optional<InvoiceEntity> opt = invoiceRepository.findById(id);
        InvoiceEntity inv = opt.orElse(null);
        if (inv == null) return ResponseEntity.status(404).body(new ErrorDTO("Không tìm thấy hóa đơn"));
        Long uCompany = user.getCompanyId();
        Long iCompany = inv.getCompanyId() != null ? inv.getCompanyId().longValue() : null;
        if (iCompany != null && !uCompany.equals(iCompany)) {
            return ResponseEntity.status(403).body(new ErrorDTO("Không có quyền thao tác hóa đơn của công ty khác"));
        }
        SignatureVatEntity sig = signatureVatRepository
                .findTopByInvoiceIdAndCompanyIdOrderByIdDesc(inv.getId().intValue(), uCompany.intValue())
                .orElse(null);
        if (sig == null || sig.getXml() == null || sig.getXml().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Hóa đơn chưa được ký số"));
        }
        String xmlToSend = normalizeInvoiceXml(sig.getXml());
        if (!xmlToSend.equals(sig.getXml())) {
            sig.setXml(xmlToSend);
            sig.setUpdatedAt(java.time.LocalDateTime.now());
            signatureVatRepository.save(sig);
        }
        // Determine message code based on form.haveCode
        Integer messageType = 200; String description;
        FormInvoiceEntity form = null;
        try {
            Long formId = inv.getFormId() != null ? inv.getFormId().longValue() : null;
            if (formId != null) form = formInvoiceRepository.findById(formId).orElse(null);
        } catch (Exception ignore) {}
        Integer have = form != null ? InvoiceTemplateBindingService.frozenForm(inv, form).getHaveCode() : null;
        if (have == null || have == 0) { messageType = 203; description = "Mã thông điệp 203"; }
        else { messageType = 200; description = "Mã thông điệp 200"; }

        // Save initial history row for sending (type 200/203)
        HistoryDto h = new HistoryDto();
        h.setCompanyId(uCompany);
        h.setUserId(user.getId());
        h.setTableName("invoices");
        h.setTableId(inv.getId().longValue());
        h.setTitle("Gửi hóa đơn giá trị gia tăng lên cơ quan thuế");
        h.setDescription(description);
        h.setShowNotify(1);
        h.setStatus(1);
        h.setType(messageType);
        h.setXmlData(xmlToSend);
        try { historyService.save(h); } catch (Exception e) { log.warn("History save failed for invoice {}: {}", id, e.toString()); }

        CompanyEntity company = companyRepository.findById(uCompany).orElse(null);
        String taxCode = company != null ? company.getTaxcode() : null;
        PatvanMessageType sendType = (have == null || have == 0) ? PatvanMessageType.HDKM : PatvanMessageType.HD;
        PatvanPostResult patvanResult;
        if (patvanCallbackService.isLocalSimulation()) {
            String localMessageCode = "LOCAL-" + java.util.UUID.randomUUID().toString().replace("-", "");
            patvanResult = new PatvanPostResult(200, "Giả lập truyền nhận local", localMessageCode, "");
        } else {
            try {
                patvanResult = patvanTransmissionService.postXml(sendType, xmlToSend, taxCode);
            } catch (PatvanTransmissionService.PatvanException e) {
                log.error("Send invoice {} to PATVAN UAT failed: {}", id, e.getMessage(), e);
                return ResponseEntity.badRequest().body(new ErrorDTO(e.getMessage()));
            }
        }

        // Cập nhật invoice status to 2 (đã gửi thuế) và lưu mã thông điệp truyền nhận
        inv.setCode(patvanResult.maThongdiep());
        inv.setStatus((short)2);
        inv.setUpdatedAt(java.time.LocalDateTime.now());
        invoiceRepository.save(inv);

        // Local tự sinh callback CQT sau khi mã thông điệp đã được lưu. Production mặc định tắt.
        if (patvanCallbackService.isLocalSimulation()) {
            patvanCallbackService.simulateInvoiceResult(
                    patvanResult.maThongdiep(), taxCode, have != null && have == 1, xmlToSend);
        }

        java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("id", inv.getId());
        resp.put("status", 2);
        resp.put("maThongdiep", patvanResult.maThongdiep());
        resp.put("message", patvanResult.message());
        return ResponseEntity.ok(resp);
    }

    private String safeStr(Object o) { return o == null ? "" : String.valueOf(o); }

    private String buildPublicLookupLink(String lookupCode, String sellerTaxcode) {
        String code = encodeQuery(lookupCode);
        String taxcode = encodeQuery(sellerTaxcode);
        if (taxcode.isBlank()) {
            return "/lookup-invoice?code=" + code;
        }
        return "/lookup-invoice?code=" + code + "&taxcode=" + taxcode;
    }

    private String encodeQuery(String value) {
        if (value == null) return "";
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String encodePath(String value) {
        return encodeQuery(value).replace("+", "%20");
    }

    private String buildPublicInvoiceFileLink(String lookupCode, String suffix, String sellerTaxcode) {
        String url = "/v1/invoices/" + encodePath(lookupCode) + suffix;
        String taxcode = encodeQuery(sellerTaxcode);
        return taxcode.isBlank() ? url : url + "?taxcode=" + taxcode;
    }

    private CompanyEntity resolveInvoiceCompany(InvoiceEntity inv, FormInvoiceEntity form) {
        Long companyId = null;
        if (form != null && form.getCompanyId() != null) {
            companyId = form.getCompanyId();
        } else if (inv != null && inv.getCompanyId() != null) {
            companyId = inv.getCompanyId().longValue();
        }
        if (companyId == null) return null;
        return companyRepository.findById(companyId).orElse(null);
    }

    private CompanyBankEntity resolveCompanyBank(CompanyEntity company) {
        if (company == null) return null;
        java.util.List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
        return banks != null && !banks.isEmpty() ? banks.get(0) : null;
    }

    private ResponseEntity<?> validatePublicInvoiceAccess(String sellerTaxcode, CompanyEntity company, MediaType errorContentType) {
        String expected = normalizeTaxcode(company != null ? company.getTaxcode() : null);
        if (expected.isBlank()) {
            return publicInvoiceAccessError(404, "Không tìm thấy hóa đơn", errorContentType);
        }
        String actual = normalizeTaxcode(sellerTaxcode);
        if (actual.isBlank()) {
            return publicInvoiceAccessError(400, "Vui lòng nhập mã số thuế bên bán", errorContentType);
        }
        if (!expected.equals(actual)) {
            return publicInvoiceAccessError(404, "Không tìm thấy hóa đơn phù hợp với mã số thuế", errorContentType);
        }
        return null;
    }

    private String normalizeTaxcode(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("[\\s.\\-]", "").toUpperCase();
    }

    private ResponseEntity<?> publicInvoiceAccessError(int status, String message, MediaType contentType) {
        if (MediaType.TEXT_HTML.equals(contentType)) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body>" + escapeHtml(message) + "</body></html>");
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_PLAIN)
                .body(message);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> history(@PathVariable("id") Long id, @AuthenticationPrincipal UserEntity user) {
        if (id == null) return ResponseEntity.badRequest().body(new ErrorDTO("Thiếu ID hóa đơn"));
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(403).build();
        }
        Optional<InvoiceEntity> opt = invoiceRepository.findById(id);
        InvoiceEntity inv = opt.orElse(null);
        if (inv == null) return ResponseEntity.status(404).body(new ErrorDTO("Không tìm thấy hóa đơn"));
        Long invCompany = inv.getCompanyId() != null ? inv.getCompanyId().longValue() : null;
        if (invCompany == null || !user.getCompanyId().equals(invCompany)) {
            return ResponseEntity.status(403).body(new ErrorDTO("Không có quyền xem lịch sử hóa đơn của công ty khác"));
        }
        
        // Dùng HistoryService để truy vấn hiệu quả
        if (historyService == null) {
            // Dự phòng về cách xử lý cũ nếu service không khả dụng
            java.util.List<vn.hoadon.entity.HistoryEntity> all = java.util.Collections.emptyList();
            try { all = historyRepository.findAll(); } catch (Exception ignore) {}
            java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
            for (vn.hoadon.entity.HistoryEntity h : all) {
                if (h == null) continue;
                if (!"invoices".equals(h.getTableName())) continue;
                if (!id.equals(h.getTableId())) continue;
                if (h.getStatus() == null || h.getStatus() != 1) continue;
                java.util.Map<String,Object> row = new java.util.LinkedHashMap<>();
                row.put("id", h.getId());
                row.put("companyId", h.getCompanyId());
                row.put("userId", h.getUserId());
                row.put("tableName", h.getTableName());
                row.put("tableId", h.getTableId());
                row.put("title", h.getTitle());
                row.put("description", h.getDescription());
                row.put("showNotify", h.getShowNotify());
                row.put("status", h.getStatus());
                row.put("type", h.getType());
                row.put("xmlData", h.getXmlData());
                row.put("createdAt", h.getCreatedAt());
                out.add(row);
            }
            out.sort((a,b) -> {
                java.time.LocalDateTime ca = (java.time.LocalDateTime) a.get("createdAt");
                java.time.LocalDateTime cb = (java.time.LocalDateTime) b.get("createdAt");
                if (ca == null && cb == null) return 0;
                if (ca == null) return 1;
                if (cb == null) return -1;
                return cb.compareTo(ca);
            });
            return ResponseEntity.ok(out);
        }
        
        // Dùng HistoryService
        List<HistoryDto> rows = historyService.listByInvoice(user.getCompanyId(), id);
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/{id}/send-email")
    public ResponseEntity<?> sendEmailNotice(@PathVariable("id") Long id,
                                             @AuthenticationPrincipal UserEntity user,
                                             @RequestBody java.util.Map<String, Object> body) {
        if (id == null) return ResponseEntity.badRequest().body(new ErrorDTO("Thiếu ID hóa đơn"));
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Không xác định được người dùng/công ty"));
        }
        InvoiceEntity inv = invoiceRepository.findById(id).orElse(null);
        if (inv == null) return ResponseEntity.status(404).body(new ErrorDTO("Không tìm thấy hóa đơn"));
        Long uCompany = user.getCompanyId();
        Long iCompany = inv.getCompanyId() != null ? inv.getCompanyId().longValue() : null;
        if (iCompany != null && !uCompany.equals(iCompany)) {
            return ResponseEntity.status(403).body(new ErrorDTO("Không có quyền thao tác hóa đơn của công ty khác"));
        }
        // Chỉ cho phép gửi với hóa đơn đã phát hành
        Short st = inv.getStatus();
        if (st == null || st.shortValue() != 3) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Hóa đơn chưa phát hành"));
        }
        String name = getString(body.get("name"));
        String email = getString(body.get("email"));
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Vui lòng nhập tên người nhận"));
        }
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Vui lòng nhập email người nhận"));
        }
        if (!isValidEmail(email)) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Email không đúng định dạng"));
        }
        try {
            String formSerial = safeStr(inv.getFormId()); // We'll try to get formCode+serial
            try {
                Long formId = inv.getFormId() != null ? inv.getFormId().longValue() : null;
                if (formId != null) {
                    FormInvoiceEntity form = formInvoiceRepository.findById(formId).orElse(null);
                    if (form != null) formSerial = (safeStr(form.getFormCode()) + safeStr(form.getSerial()));
                }
            } catch (Exception ignore) {}
            String subject = "Thông báo phát hành hóa đơn số " + safeStr(inv.getNo()) + (formSerial != null && !formSerial.isEmpty() ? " (" + formSerial + ")" : "");
            String lookupCode = inv.getLookupCode() != null ? inv.getLookupCode() : String.valueOf(inv.getId());

            // Fetch company info for template variables
            String companyName = "", hotline = "", comEmail = "", website = "", sellerTaxcode = "";
            try {
                vn.hoadon.entity.CompanyEntity company = companyRepository.findById(uCompany).orElse(null);
                if (company != null) {
                    if (company.getName() != null)           companyName = company.getName();
                    if (company.getTaxcode() != null)        sellerTaxcode = company.getTaxcode();
                    if (company.getHotline() != null)        hotline     = company.getHotline();
                    if (company.getInvoiceEmail() != null)   comEmail    = company.getInvoiceEmail();
                    else if (company.getEmail() != null)     comEmail    = company.getEmail();
                    if (company.getInvoiceWebsite() != null) website     = company.getInvoiceWebsite();
                }
            } catch (Exception ignore) {}
            String pdfLink = buildPublicInvoiceFileLink(lookupCode, "/download-pdf", sellerTaxcode);
            String xmlLink = buildPublicInvoiceFileLink(lookupCode, "/download-xml", sellerTaxcode);

            java.util.Map<String, Object> customerMap = java.util.Collections.emptyMap();
            try {
                String custJson = inv.getCustomer();
                if (custJson != null && !custJson.isBlank()) {
                    customerMap = JSON.readValue(custJson,
                            new TypeReference<java.util.Map<String, Object>>() {});
                }
            } catch (Exception ignore) {}
            String invoiceCustomerName = invoiceCustomerCompanyName(customerMap);
            String buyerName = invoiceCustomerBuyerName(customerMap);
            String customerName = firstNonBlankText(invoiceCustomerName, buyerName, name.trim(), "Quý khách");
            String taxCode = firstNonBlankText(customerMap.get("tax_code"), customerMap.get("taxcode"), customerMap.get("taxCode"));
            String html = buildEmailHtml(inv, formSerial, pdfLink, xmlLink, customerName);

            MailJobMessage msg = new MailJobMessage();
            msg.setTemplateKey("ISSUE_INVOICE_MAIL");
            msg.setCompanyId(uCompany);
            msg.setToEmail(email.trim());
            msg.setToName(name.trim());
            java.util.Map<String, String> vars = new java.util.HashMap<>();
            vars.put("SO_HOA_DON",  inv.getNo() != null ? String.valueOf(inv.getNo()) : "");
            vars.put("CUS_NAME",    customerName);
            vars.put("CUSTOMER_NAME", customerName);
            vars.put("BUYER_NAME",  buyerName);
            vars.put("CUS_BUYER",   buyerName);
            vars.put("RECIPIENT_NAME", name.trim());
            vars.put("TO_NAME",     name.trim());
            vars.put("CUS_TAXCODE", taxCode);
            vars.put("LOOKUP_LINK", buildPublicLookupLink(lookupCode, sellerTaxcode));
            vars.put("LOOKUP_CODE", lookupCode);
            vars.put("PDF_LINK",    pdfLink);
            vars.put("XML_LINK",    xmlLink);
            vars.put("COM_NAME",    companyName);
            vars.put("COM_HOTLINE", hotline);
            vars.put("COM_EMAIL",   comEmail);
            vars.put("COM_WEBSITE", website);
            // Dự phòng nếu không tìm thấy template trong DB
            vars.put("SUBJECT",  subject);
            vars.put("HTML_BODY", html);
            addInvoiceZipAttachmentVars(vars, inv);
            msg.setVariables(vars);
            msg.setInvoiceId(inv.getId() != null ? inv.getId().longValue() : null);

            boolean sent = false;
            if (mailQueueService != null) {
                try {
                    mailQueueService.enqueue(msg);
                    sent = true;
                } catch (Exception queueEx) {
                    log.warn("[send-email] Queue unavailable ({}), falling back to direct send", queueEx.getMessage());
                }
            }
            if (!sent && mailWorker != null) {
                mailWorker.processSync(msg);
                sent = true;
            }
            if (!sent) {
                log.warn("[send-email] No mail sender available for invoice {}", id);
                return ResponseEntity.status(500).body(new ErrorDTO("Chưa cấu hình mail server"));
            }

            java.util.Map<String,Object> resp = new java.util.LinkedHashMap<>();
            resp.put("message", "Đã gửi email thông báo phát hành hóa đơn");
            resp.put("email", email);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.warn("Send email notice failed for invoice {}: {}", id, e.toString());
            return ResponseEntity.status(500).body(new ErrorDTO("Gửi email thất bại"));
        }
    }

    private String buildEmailHtml(InvoiceEntity inv, String formSerial, String pdfLink, String xmlLink, String receiverName) {
        String no = safeStr(inv.getNo());
        String date = safeStr(inv.getDateExport());
        String amount = safeStr(inv.getAmount());
        String codeCqt = safeStr(inv.getCodeCqt());
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Arial,Helvetica,sans-serif;font-size:14px;color:#222'>");
        sb.append("<p>Xin chào ").append(escapeHtml(receiverName)).append(",</p>");
        sb.append("<p>Hóa đơn của bạn đã được phát hành.</p>");
        sb.append("<table style='border-collapse:collapse'>");
        appendRow(sb, "Ký hiệu", escapeHtml(formSerial));
        appendRow(sb, "Số hóa đơn", escapeHtml(no));
        appendRow(sb, "Ngày lập", escapeHtml(date));
        appendRow(sb, "Tổng tiền", escapeHtml(amount));
        if (codeCqt != null && !codeCqt.isEmpty()) appendRow(sb, "Mã CQT", escapeHtml(codeCqt));
        sb.append("</table>");
        if (pdfLink != null && !pdfLink.isEmpty()) {
            sb.append("<p><a href='").append(escapeHtml(pdfLink)).append("'>Tải PDF</a></p>");
        }
        if (xmlLink != null && !xmlLink.isEmpty()) {
            sb.append("<p><a href='").append(escapeHtml(xmlLink)).append("'>Tải XML</a></p>");
        }
        sb.append("<p>Trân trọng.</p>");
        sb.append("</div>");
        return sb.toString();
    }
    private void appendRow(StringBuilder sb, String k, String v) {
        sb.append("<tr><td style='padding:4px 8px;color:#555'>").append(escapeHtml(k)).append(":</td><td style='padding:4px 8px'><b>").append(escapeHtml(v)).append("</b></td></tr>");
    }

    private boolean isValidEmail(String email) {
        try {
            String e = email == null ? null : email.trim();
            if (e == null || e.isEmpty()) return false;
            return e.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        } catch (Exception ignore) { return false; }
    }
    private String getString(Object o) { return o == null ? null : String.valueOf(o); }

    private String getInvoiceXmlByStatus(InvoiceEntity inv, FormInvoiceEntity form, CompanyEntity company, CompanyBankEntity bank) {
        return invoiceDocuments.xml(inv, form).content();
    }

    private String normalizeInvoiceXml(String xml) {
        return InvoiceXmlTaxValidator.addDefaultValues(xml);
    }

}
