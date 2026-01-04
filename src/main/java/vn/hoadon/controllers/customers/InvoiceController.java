package vn.hoadon.controllers.customers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.dto.InvoiceDTO;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.RegisterInvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.RegisterInvoiceRepository;
import vn.hoadon.services.InvoiceService;

@RestController
@RequestMapping("/v1/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private FormInvoiceRepository formInvoiceRepository;
    @Autowired
    private RegisterInvoiceRepository registerInvoiceRepository;

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
        // 2) Accepted register invoice (status=6), pick latest by effective_date
        var pages = registerInvoiceRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, 6, PageRequest.of(0, 50));
        RegisterInvoiceEntity latestAccepted = null;
        for (RegisterInvoiceEntity ri : pages.getContent()) {
            if (latestAccepted == null || (ri.getEffectiveDate() != null && (latestAccepted.getEffectiveDate() == null || ri.getEffectiveDate().isAfter(latestAccepted.getEffectiveDate())))) {
                latestAccepted = ri;
            }
        }
        if (latestAccepted == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Chưa có tờ khai được chấp nhận"));
        }
        // 3) Vat category requires invoice_types contain HDGTGT
        if (activeVatForm.getCategory() != null && activeVatForm.getCategory() == 1) {
            String invoiceTypes = safeString(latestAccepted.getInvoiceTypes());
            if (!containsToken(invoiceTypes, "HDGTGT")) {
                return ResponseEntity.badRequest().body(new ErrorDTO("Tờ khai không đăng ký loại hóa đơn GTGT (HDGTGT)"));
            }
        }
        // 4) Check invoice_forms CMa/KCMa depending on haveCode
        String invoiceForms = safeString(latestAccepted.getInvoiceForms());
        Integer haveCode = activeVatForm.getHaveCode();
        boolean requireCMa = haveCode != null && haveCode == 1;
        boolean okForm = requireCMa ? containsToken(invoiceForms, "CMa") : containsToken(invoiceForms, "KCMa");
        if (!okForm) {
            return ResponseEntity.badRequest().body(new ErrorDTO(requireCMa ? "Tờ khai không đăng ký hình thức Cấp mã (CMa)" : "Tờ khai không đăng ký hình thức Không cấp mã (KCMa)"));
        }
        // OK
        PrepareDTO dto = new PrepareDTO();
        dto.formId = activeVatForm.getId();
        dto.formCode = activeVatForm.getFormCode();
        dto.serial = activeVatForm.getSerial();
        dto.haveCode = activeVatForm.getHaveCode();
        dto.registerId = latestAccepted.getId();
        dto.registerEffectiveDate = latestAccepted.getEffectiveDate() != null ? latestAccepted.getEffectiveDate().toString() : null;
        return ResponseEntity.ok(dto);
    }

    private static boolean containsToken(String csv, String token) {
        if (csv == null || token == null) return false;
        String[] arr = csv.split(",");
        for (String s : arr) { if (s != null && s.trim().equalsIgnoreCase(token)) return true; }
        return false;
    }
    private static String safeString(Object o) { return o == null ? null : String.valueOf(o); }

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
    public static class PrepareDTO { public Long formId; public String formCode; public String serial; public Integer haveCode; public Long registerId; public String registerEffectiveDate; }
}
