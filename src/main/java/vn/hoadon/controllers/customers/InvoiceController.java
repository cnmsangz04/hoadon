package vn.hoadon.controllers.customers;

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
import vn.hoadon.dto.InvoiceDTO;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceEntity;
import vn.hoadon.entity.RegisterInvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.InvoiceRepository;
import vn.hoadon.repositories.RegisterInvoiceRepository;
import vn.hoadon.services.InvoiceService;

@RestController
@RequestMapping("/v1/invoices")
public class InvoiceController {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private FormInvoiceRepository formInvoiceRepository;
    @Autowired
    private RegisterInvoiceRepository registerInvoiceRepository;
    @Autowired
    private InvoiceRepository invoiceRepository;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) Short status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        int pageIndex = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<InvoiceDTO> result = invoiceService.search(q, status, pageable);
        return ResponseEntity.ok(new PageDTO<>(result.getContent(), result.getNumber() + 1, result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/prepare")
    public ResponseEntity<?> prepare(@AuthenticationPrincipal UserEntity user) {
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Không xác định được công ty/người dùng"));
        }
        Long companyId = user.getCompanyId();
        // 1) Active VAT form
        FormInvoiceEntity activeVatForm = formInvoiceRepository.findTopByCompanyIdAndStatusAndCategoryOrderByUpdatedAtDesc(companyId, 1, 1);
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
        dto.formCode = activeVatForm.getFormCode();
        dto.serial = activeVatForm.getSerial();
        dto.haveCode = activeVatForm.getHaveCode();
        dto.registerId = latestAccepted.getId();
        dto.registerEffectiveDate = latestAccepted.getEffectiveDate() != null ? latestAccepted.getEffectiveDate().toString() : null;
        dto.companyId = user.getCompanyId();
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal UserEntity user, @RequestBody CreateRequest req) {
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Vui lòng xác định công ty/người dùng"));
        }
        Long companyId = user.getCompanyId();
        Long userId = user.getId();
        InvoiceService.InvoicePayload p = toPayload(req);
        vn.hoadon.entity.InvoiceEntity saved = invoiceService.create(p, companyId, userId);
        if (saved == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Không thể lưu hóa đơn"));
        }
        return ResponseEntity.ok(new CreateResponse(saved.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @AuthenticationPrincipal UserEntity user, @RequestBody CreateRequest req) {
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Vui lòng xác định công ty/người dùng"));
        }
        Long companyId = user.getCompanyId();
        Long userId = user.getId();
        InvoiceService.InvoicePayload p = toPayload(req);
        vn.hoadon.entity.InvoiceEntity saved = invoiceService.update(id, p, companyId, userId);
        if (saved == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Không tìm thấy hóa đơn để cập nhật"));
        }
        return ResponseEntity.ok(new CreateResponse(saved.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id, @AuthenticationPrincipal UserEntity user) {
        if (id == null) return ResponseEntity.badRequest().body(new ErrorDTO("Thiếu ID hóa đơn"));
        java.util.Optional<vn.hoadon.entity.InvoiceEntity> opt = invoiceRepository.findById(id);
        vn.hoadon.entity.InvoiceEntity inv = opt.orElse(null);
        if (inv == null) return ResponseEntity.badRequest().body(new ErrorDTO("Không tìm thấy hóa đơn"));
        // Build response with parsed JSON blobs
        java.util.Map<String,Object> resp = new java.util.LinkedHashMap<>();
        resp.put("id", inv.getId());
        resp.put("formId", inv.getFormId());
        resp.put("invoiceNumberId", inv.getInvoiceNumberId());
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
        return p;
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
        // If looks like JSON array, parse it
        if (s.startsWith("[") && s.endsWith("]")) {
            try {
                return JSON.readValue(s, new TypeReference<java.util.List<String>>(){});
            } catch (Exception ignore) { /* fall through to CSV */ }
        }
        // Fallback: CSV split
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
    public static class PrepareDTO { public Long formId; public String formCode; public String serial; public Integer haveCode; public Long registerId; public String registerEffectiveDate; public Long companyId; }

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
    }

    public static class CreateResponse { public Long id; public CreateResponse(Long id){ this.id = id; } }
}