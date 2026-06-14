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

import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.InvoiceDTO;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceEntity;
import vn.hoadon.entity.RegisterInvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.CompanyBankEntity;
import vn.hoadon.entity.BuyInvoiceEntity;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.InvoiceRepository;
import vn.hoadon.repositories.RegisterInvoiceRepository;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.CompanyBankRepository;
import vn.hoadon.repositories.InvoiceNumberRepository;
import vn.hoadon.repositories.SignatureVatRepository;
import vn.hoadon.repositories.BuyInvoiceRepository;
import vn.hoadon.entity.InvoiceNumberEntity;
import vn.hoadon.entity.SignatureVatEntity;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.services.InvoiceService;
import vn.hoadon.services.HistoryService;
import vn.hoadon.services.SignatureAuthoritiesTaxService;
import vn.hoadon.dto.history.HistoryDto;
import vn.hoadon.dto.SignatureAuthoritiesTaxDTO;

import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.List;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.extend.FSUriResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.hoadon.repositories.HistoryRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

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
    @Autowired private HistoryService historyService;
    @Autowired private SignatureAuthoritiesTaxService signatureAuthoritiesTaxService;
    @Autowired private HistoryRepository historyRepository;
    @Autowired(required = false) private JavaMailSender mailSender;
    
    @Autowired @org.springframework.context.annotation.Lazy private InvoiceController self;

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

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
        
        // If user doesn't have companyId (which shouldn't happen), return empty result
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
    	permission("invoice-save");
    	
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
    	permission("invoice-save");
    	
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

    @GetMapping(value = "/{lookup}/view", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> viewByLookup(@PathVariable("lookup") String lookup) {
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
        String xsltValue = form != null ? form.getFile() : null;
        if (!org.springframework.util.StringUtils.hasText(xsltValue)) {
            log.warn("Invoice view: missing XSLT file value. lookup={}, formId={}", lookup, inv.getFormId());
            return ResponseEntity.status(404).contentType(MediaType.TEXT_HTML).body("<html><body>Không tìm thấy tệp XSLT của mẫu</body></html>");
        }
        boolean looksLikeXslt = looksLikeInlineXslt(xsltValue);
        Path xsltFsPath = looksLikeXslt ? null : toFilesystemPath(xsltValue);
        if (!looksLikeXslt && (xsltFsPath == null || !Files.exists(xsltFsPath))) {
            log.warn("Invoice view: XSLT file not found on system. lookup={}, formId={}, xsltPublicPath={}, resolvedPath={}", lookup, inv.getFormId(), xsltValue, xsltFsPath);
            return ResponseEntity.status(404).contentType(MediaType.TEXT_HTML).body("<html><body>Không tồn tại tệp XSLT trên hệ thống</body></html>");
        }

        CompanyEntity company = null; CompanyBankEntity bank = null;
        if (form != null && form.getCompanyId() != null) {
            company = companyRepository.findById(form.getCompanyId()).orElse(null);
            if (company != null) {
                java.util.List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
                if (banks != null && !banks.isEmpty()) bank = banks.get(0);
            }
        }
        String xml = getInvoiceXmlByStatus(inv, form, company, bank);
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            try { factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true); } catch (Exception ignored) {}
            try { factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "file"); } catch (Exception ignored) {}
            StreamSource xsltSource = looksLikeXslt
                    ? new StreamSource(new StringReader(xsltValue))
                    : new StreamSource(xsltFsPath.toFile());
            Transformer transformer = factory.newTransformer(xsltSource);
            StreamSource xmlSource = new StreamSource(new StringReader(xml));
            StringWriter outWriter = new StringWriter();
            StreamResult result = new StreamResult(outWriter);
            transformer.transform(xmlSource, result);
            String html = outWriter.toString();
            // Resolve named HTML entities to avoid XML parser errors in the renderer
            html = resolveNamedHtmlEntities(html);
            // Đảm bảo meta UTF-8 cho trình duyệt và bộ xử lý phía sau
            html = ensureUtf8Meta(html);
            // Ép thay font-family thường thiếu glyph tiếng Việt
            html = forceReplaceFontFamilies(html);
            // Normalize to XHTML-ish by self-closing void tags like <meta>, <link>, <img>, etc.
            html = normalizeToXhtml(html);
            // Inject a simulated QR code into placeholders if present
            html = injectQrPlaceholder(html);
            // Sanitize các giá trị <img src> có thể chứa '<' thô trước khi gửi sang renderer
            html = sanitizeImgSrcAttributes(html);
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        } catch (Exception ex) {
            String msg = "<html><body>Lỗi xử lý mẫu: " + escapeHtml(ex.getMessage()) + "</body></html>";
            return ResponseEntity.status(500).contentType(MediaType.TEXT_HTML).body(msg);
        }
    }

    @GetMapping(value = "/{lookup}/download-xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> downloadXmlByLookup(@PathVariable("lookup") String lookup) {
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
        CompanyEntity company = null; CompanyBankEntity bank = null;
        if (form != null && form.getCompanyId() != null) {
            company = companyRepository.findById(form.getCompanyId()).orElse(null);
            if (company != null) {
                java.util.List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
                if (banks != null && !banks.isEmpty()) bank = banks.get(0);
            }
        }
        String xml = getInvoiceXmlByStatus(inv, form, company, bank);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("invoice-" + lookup + ".xml").build());
        return new ResponseEntity<>(xml, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/{lookup}/download-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> downloadPdfByLookup(@PathVariable("lookup") String lookup) {
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
        String xsltValue = form != null ? form.getFile() : null;
        if (!org.springframework.util.StringUtils.hasText(xsltValue)) {
            log.warn("Invoice pdf: missing XSLT file value. lookup={}, formId={}", lookup, inv.getFormId());
            return ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN).body("Không tìm thấy tệp XSLT của mẫu");
        }
        boolean looksLikeXslt = looksLikeInlineXslt(xsltValue);
        Path xsltFsPath = looksLikeXslt ? null : toFilesystemPath(xsltValue);
        if (!looksLikeXslt && (xsltFsPath == null || !Files.exists(xsltFsPath))) {
            log.warn("Invoice pdf: XSLT file not found on system. lookup={}, formId={}, xsltPublicPath={}, resolvedPath={}", lookup, inv.getFormId(), xsltValue, xsltFsPath);
            return ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN).body("Không tồn tại tệp XSLT trên hệ thống");
        }
        CompanyEntity company = null; CompanyBankEntity bank = null;
        if (form != null && form.getCompanyId() != null) {
            company = companyRepository.findById(form.getCompanyId()).orElse(null);
            if (company != null) {
                java.util.List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
                if (banks != null && !banks.isEmpty()) bank = banks.get(0);
            }
        }
        String xml = getInvoiceXmlByStatus(inv, form, company, bank);
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            try { factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true); } catch (Exception ignored) {}
            try { factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "file"); } catch (Exception ignored) {}
            StreamSource xsltSource = looksLikeXslt
                    ? new StreamSource(new StringReader(xsltValue))
                    : new StreamSource(xsltFsPath.toFile());
            Transformer transformer = factory.newTransformer(xsltSource);
            StreamSource xmlSource = new StreamSource(new StringReader(xml));
            StringWriter outWriter = new StringWriter();
            StreamResult result = new StreamResult(outWriter);
            transformer.transform(xmlSource, result);
            String html = outWriter.toString();
            html = resolveNamedHtmlEntities(html);
            // Chèn meta UTF-8 và font fallback chắc chắn để tránh thiếu glyph
            html = ensureUtf8Meta(html);
            html = forceReplaceFontFamilies(html);
            html = normalizeToXhtml(html);
            html = injectQrPlaceholder(html);
            html = sanitizeImgSrcAttributes(html);

            // Apply PDF-specific layout and font fallbacks similar to FileController
            html = ensurePdfLayoutFallbackCss(html);
            html = ensurePdfCss(html);
            html = ensurePdfFontCss(html);
            html = ensurePdfTypoCss(html);

            ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            String baseUri = null;
            if (!looksLikeXslt && xsltFsPath != null && xsltFsPath.getParent() != null) {
                baseUri = xsltFsPath.getParent().toUri().toString();
            }
            builder.useFastMode();
            // Prefer screen media type for closer parity
            try {
                java.lang.reflect.Method m = PdfRendererBuilder.class.getMethod("useMediaType", String.class);
                m.invoke(builder, "screen");
            } catch (Exception ignore) {
                try {
                    java.lang.reflect.Method m2 = PdfRendererBuilder.class.getMethod("useScreenMediaType", boolean.class);
                    m2.invoke(builder, true);
                } catch (Exception ignore2) {}
            }
            // Use same URI resolver as FileController
            builder.useUriResolver(new ClasspathFirstUriResolver());
            // Register available Unicode fonts to fix missing glyphs
            registerAvailableUnicodeFonts(builder);

            if (baseUri != null) builder.withHtmlContent(html, baseUri); else builder.withHtmlContent(html, null);
            builder.toStream(pdfOut);
            builder.run();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.attachment().filename("invoice-" + lookup + ".pdf").build());
            return new ResponseEntity<>(pdfOut.toByteArray(), headers, HttpStatus.OK);
        } catch (javax.xml.transform.TransformerConfigurationException tce) {
            return ResponseEntity.status(500).contentType(MediaType.TEXT_PLAIN).body("Lỗi cấu hình XSLT: " + tce.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).contentType(MediaType.TEXT_PLAIN).body("Lỗi tạo PDF: " + ex.getMessage());
        }
    }

    @PostMapping("/{id}/sign")
    @Transactional
    public ResponseEntity<?> sign(@PathVariable("id") Long id, @AuthenticationPrincipal UserEntity user) {
        if (id == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Thiếu ID hóa đơn"));
        }
        Optional<InvoiceEntity> optInv = invoiceRepository.findById(id);
        InvoiceEntity inv = optInv.orElse(null);
        if (inv == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Không tìm thấy hóa đơn"));
        }
        // Permission: if user provided, ensure same company
        if (user != null && user.getCompanyId() != null && inv.getCompanyId() != null) {
            Long uCompany = user.getCompanyId();
            Long iCompany = inv.getCompanyId().longValue();
            if (!uCompany.equals(iCompany)) {
                return ResponseEntity.status(403).body(new ErrorDTO("Không có quyền ký số hóa đơn của công ty khác"));
            }
        }
        // 1) Ensure there is an active invoice_numbers row for this form_id; create if missing
        Long companyId = inv.getCompanyId() != null ? inv.getCompanyId().longValue() : (user != null ? user.getCompanyId() : null);
        if (companyId == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Không xác định được công ty của hóa đơn"));
        }
        Long formId = inv.getFormId() != null ? inv.getFormId().longValue() : null;
        if (formId == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Thiếu mẫu hóa đơn (form_id)"));
        }
        
        // Check buy_invoices: must have active record with available invoices
        Optional<BuyInvoiceEntity> optBuyInvoice = buyInvoiceRepository.findFirstByCompanyIdAndStatusOrderByIdDesc(companyId, 1);
        if (!optBuyInvoice.isPresent()) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Công ty chưa có gói hóa đơn được kích hoạt"));
        }
        BuyInvoiceEntity buyInvoice = optBuyInvoice.get();
        
        // Validate amount_used < amount
        Integer amount = buyInvoice.getAmount() != null ? buyInvoice.getAmount() : 0;
        Integer amountUsed = buyInvoice.getAmountUsed() != null ? buyInvoice.getAmountUsed() : 0;
        
        if (amountUsed >= amount) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Quý khách đã sử dụng hết số hóa đơn"));
        }
        
        // Find active invoice number counter for VAT category (1) with status=1
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
        // Allocate next number: total + 1
        Integer nextNo = (counter.getTotal() != null ? counter.getTotal() : 0) + 1;
        counter.setTotal(nextNo);
        counter.setUpdatedAt(java.time.LocalDateTime.now());
        invoiceNumberRepository.save(counter);
        
        // Increment buy_invoice amount_used
        buyInvoice.setAmountUsed(amountUsed + 1);
        buyInvoice.setUpdatedAt(java.time.LocalDateTime.now());
        buyInvoiceRepository.save(buyInvoice);
        
        // 2) Update invoice: set invoice_number_id if missing, set no, set status=1 (Đã ký)
        if (inv.getInvoiceNumberId() == null) {
            inv.setInvoiceNumberId(counter.getId().intValue());
        }
        inv.setNo(nextNo);
        inv.setStatus((short)1);
        inv.setUpdatedAt(java.time.LocalDateTime.now());
        invoiceRepository.save(inv);
        // 3) Build XML with company info and bank, using current form
        FormInvoiceEntity form = null;
        try {
            form = formInvoiceRepository.findById(formId).orElse(null);
        } catch (Exception ignore) {}
        CompanyEntity company = null; CompanyBankEntity bank = null;
        if (form != null && form.getCompanyId() != null) {
            company = companyRepository.findById(form.getCompanyId()).orElse(null);
            if (company != null) {
                java.util.List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
                if (banks != null && !banks.isEmpty()) bank = banks.get(0);
            }
        } else {
            // Fallback by invoice.company_id
            company = companyRepository.findById(companyId).orElse(null);
            if (company != null) {
                java.util.List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
                if (banks != null && !banks.isEmpty()) bank = banks.get(0);
            }
        }
        String xml = vn.hoadon.util.InvoiceXmlBuilder.build(inv, form, company, bank);
        // Inject mock digital signature into <DSCKS><NBan>...</NBan></DSCKS>
        xml = injectMockSignature(xml, company);
        // 4) Persist signature_vats with xml
        SignatureVatEntity sig = new SignatureVatEntity();
        sig.setCompanyId(companyId.intValue());
        sig.setInvoiceId(inv.getId().intValue());
        sig.setXml(xml);
        sig.setCreatedAt(java.time.LocalDateTime.now());
        sig.setUpdatedAt(java.time.LocalDateTime.now());
        signatureVatRepository.save(sig);
        // Response: return new number
        java.util.Map<String,Object> resp = new java.util.LinkedHashMap<>();
        resp.put("id", inv.getId());
        resp.put("no", nextNo);
        resp.put("status", 1);
        return ResponseEntity.ok(resp);
    }

    // Build and inject a mock XMLDSIG signature under <DSCKS><NBan>...</NBan></DSCKS>
    private String injectMockSignature(String xml, CompanyEntity company) {
        if (xml == null || xml.isBlank()) return xml;
        String id = extractDlhDonId(xml);
        if (id == null || id.isBlank()) {
            id = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        }
        String signingTimeId = "SigningTime-NNT-" + id;
        String sigPropId = "SignatureProperty-" + id;
        String now = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String subjectName = buildX509SubjectName(company);
        String certificate = SAMPLE_CERT_BASE64;
        String signatureValue = SAMPLE_SIGNATURE_VALUE;
        String digest1 = SAMPLE_DIGEST_VALUE_1;
        String digest2 = SAMPLE_DIGEST_VALUE_2;
        String dsig = "<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">" +
                "<SignedInfo>" +
                "<CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\"/>" +
                "<SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>" +
                "<Reference URI=\"#" + escapeXml(id) + "\">" +
                "<DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>" +
                "<DigestValue>" + digest1 + "</DigestValue>" +
                "</Reference>" +
                "<Reference URI=\"#" + escapeXml(signingTimeId) + "\">" +
                "<DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>" +
                "<DigestValue>" + digest2 + "</DigestValue>" +
                "</Reference>" +
                "</SignedInfo>" +
                "<SignatureValue>" + signatureValue + "</SignatureValue>" +
                "<KeyInfo><X509Data>" +
                "<X509SubjectName>" + escapeXml(subjectName) + "</X509SubjectName>" +
                "<X509Certificate>" + certificate + "</X509Certificate>" +
                "</X509Data></KeyInfo>" +
                "<Object Id=\"" + escapeXml(signingTimeId) + "\">" +
                "<SignatureProperties>" +
                "<SignatureProperty Id=\"" + escapeXml(sigPropId) + "\" Target=\"#NNT-" + escapeXml(id) + "\">" +
                "<SigningTime>" + escapeXml(now) + "</SigningTime>" +
                "</SignatureProperty>" +
                "</SignatureProperties>" +
                "</Object>" +
                "</Signature>";
        String dscks = "<DSCKS><NBan>" + dsig + "</NBan><NMua/><CQT/><CCKSKhac/></DSCKS>";
        // Replace existing DSCKS block if present; else insert before </HDon>
        String replaced = xml.replaceFirst("(?is)<DSCKS>.*?</DSCKS>", java.util.regex.Matcher.quoteReplacement(dscks));
        if (replaced.equals(xml)) {
            // No DSCKS found, try to insert before closing tag
            replaced = xml.replaceFirst("(?is)</HDon>\s*$", java.util.regex.Matcher.quoteReplacement(dscks) + "</HDon>");
        }
        return replaced;
    }

    private String extractDlhDonId(String xml) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?is)<DLHDon\\s+Id=\\\"([A-Za-z0-9_-]+)\\\"").matcher(xml);
            if (m.find()) return m.group(1);
        } catch (Exception ignore) {}
        return null;
    }

    private String buildX509SubjectName(CompanyEntity company) {
        String mst = company != null && company.getTaxcode() != null ? company.getTaxcode() : "MST:0000000000";
        String cn = company != null && company.getName() != null ? company.getName() : "CONG TY";
        // Keep it simple and deterministic for mock
        return "UID=MST:" + mst + ",CN=" + cn + ",O=" + cn + ",C=VN";
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    // Static sample values for mock signature; not cryptographically valid but structurally similar
    private static final String SAMPLE_SIGNATURE_VALUE = "dSPVczz7SoqFQeGu1BWW3SKj8Z2AFx0hIuDtk3mSxoP3wTov3pAvO3FBTFBozPe5Bbe0SEI8ePTFAHzwxy77tL9bMQDN6YQfK4re9G1G2QYuuvfOD7TuPp+B4jPKqTcCrPdQBDa5tZ64ZHZzrr2CcWFyGmE1x6lXwX3jACIhdfQ5e6BmL2P4Dnb5FnJlt2+vfQaRoP/29rVMbqv0KYOcNgUCF5LWbIVxIYvwkzNK6u80T9/izHzeukJAII465YQe+LW6w+CitXn1qwTgW4FRXbtrjGMP+u/O2yl4fBxIMMRJvfjsly78XtXTmqE4M4U22pZhzgik7IdT3/VxJl2SPA==";
    private static final String SAMPLE_CERT_BASE64 = "MIIEwzCCA6ugAwIBAgIQVAESWO4VZZCsWne4hUUjfjANBgkqhkiG9w0BAQsFADArMQ0wCwYDVQQDDARJLUNBMQ0wCwYDVQQKDARJLUNBMQswCQYDVQQGEwJWTjAeFw0yNDAyMDIwNjU3MjJaFw0yNzAxMjQwODExNDlaMIG2MQswCQYDVQQGEwJWTjEXMBUGA1UECAwOSOG7kyBDaMOtIE1pbmgxEjAQBgNVBAcMCVF14bqtbiAxMDEqMCgGA1UECgwhQ8OUTkcgVFkgVE5ISCBQLkEgVknhu4ZUIE5BTSBURVNUMSowKAYDVQQDDCFDw5RORyBUYSBUTkhIIFAuQSBWSeG7hlQgTkFNIFRFU1QxIjAgBgoJkiaJk/IsZAEBDBJNU1Q6MDMwMjQzMTU5NS05OTkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDLN2b9KzjCvOG3Kt/NQOv0Y9OKs837Rk2Z6VBU4Xj24xNy4kN5xiC/ZTMBveh7GvBybx4IIp2hTdHNHiSTSeiEP99JAt2FsvLaw2NGMnk7u16PPtzRjbF0IrlWtiOj8C1kGMo8/Lv6FXhL/DFc9ny6dIw1++Qq+UkHv3hJsT385Eyu+oTWojWzHLsCykzYY9LRKhfv0w0O0tPkskdXXD/5gsxgalnR7+CdDqnOLuXQKUx/OYHaD6Di+68qIe8wkrTrj6gB4pldvweVRKrT6JX+y950jLiROP2mQRthOi5r2ZonP4TRVH2ms+bxGZIBrftKcbuSCKsvc9DB438LGp2/AgMBAAGjggFVMIIBUTAMBgNVHRMBAf8EAjAAMB8GA1UdIwQYMBaAFMgnCWzCxN9H6hnydyl7Z1xDsSkrMIGVBggrBgEFBQcBAQSBiDCBhTAyBggrBgEFBQcwAoYmaHR0cHM6Ly9yb290Y2EuZ292LnZuL2NydC92bnJjYTI1Ni5wN2IwLgYIKwYBBQUHMAKGImh0dHBzOi8vcm9vdGNhLmdvdi52bi9jcnQvSS1DQS5wN2IwHwYIKwYBBQUHMAGGE2h0dHA6Ly9vY3NwLmktY2Eudm4wNAYDVR0lBC0wKwYIKwYBBQUHAwIGCCsGAQUFBwMEBgorBgEEAYI3CgMMBgkqhkiG9y8BAQUwIwYDVR0fBBwwGjAYoBagFIYSaHR0cDovL2NybC5pLWNhLnZuMB0GA1UdDgQWBBQWJU8iR6U+0upLf4/bUqYSa+a1UTAOBgNVHQ8BAf8EBAMCBPAwDQYJKoZIhvcNAQELBQADggEBAIbaPrHjDKCp2t4DcHa9763XghY0t5EYBVm2ek5MS3nIizxMqE1jwc5dTLe+HyBdiqwCEvQxI+y3tymnZRQjWCFpnXsWghURBERv9zw0y8nAnZk6Ajby9AZ077BbIb44EzsMPvAKEFHWfSic76zaPQ7fw0pGOWXJEaby0bC0l6ho5FgP7Kc9diIx5buGqTsatWz6ynC3JxAyLclzyyh9gcSgUamBj7yEQuROZg/dCgiRD6bUsmSpDR4cPP4rzhp3SA82Gkasg0AwZKd0G8KbBIP4Mot219h+PCi/SryHEMU12106WS/SZ1/Vqiac0sUTnPvyR4Xl4uUV2aKNm+pxiCU=";
    private static final String SAMPLE_DIGEST_VALUE_1 = "BTWIVMXw2EXuSV7ThhJHGgSbbLcs2veOkk8oKwpgVzo=";
    private static final String SAMPLE_DIGEST_VALUE_2 = "2pqCy/KvD9b88B4AWHNgxIFA9YTWbKOd8cHudzk1XwE=";

    /**
     * Inject mock CQT (tax authority) signature into the XML at HDon/DSCKS/CQT
     * This simulates the tax authority signing the invoice.
     */
    private String injectCqtSignature(String xml) {
        if (xml == null || xml.isBlank()) return xml;
        
        // Extract DLHDon Id for reference
        String id = extractDlhDonId(xml);
        if (id == null || id.isBlank()) {
            id = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        }
        
        // Generate unique IDs for CQT signature
        String cqtSigId = "Tct-" + java.util.UUID.randomUUID().toString().replace("-", "");
        String signingTimeId = "SigningTime-" + java.util.UUID.randomUUID().toString().replace("-", "");
        String sigPropId = "Id-" + java.util.UUID.randomUUID().toString().replace("-", "");
        String now = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        
        // Build the CQT signature XML (using the sample provided by user)
        String cqtSignature = "<CQT>" +
            "<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"" + escapeXml(cqtSigId) + "\">" +
            "<SignedInfo>" +
            "<CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\"/>" +
            "<SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/>" +
            "<Reference URI=\"#" + escapeXml(id) + "\">" +
            "<DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>" +
            "<DigestValue>u4Hn4j2TmvMXMptS8K3iVaAP6TqH2gXvJHcFSLm8EcQ=</DigestValue>" +
            "</Reference>" +
            "<Reference URI=\"#" + escapeXml(signingTimeId) + "\">" +
            "<DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>" +
            "<DigestValue>0zGKKJY1nVCsJ76w5q2MmD9u4X0Xi33ue0AwGUE7zjk=</DigestValue>" +
            "</Reference>" +
            "<Reference URI=\"#" + escapeXml(sigPropId) + "\">" +
            "<DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>" +
            "<DigestValue>BNH0+RWN+KX2GY4cJixbmuUIzLtyp56hWAwpbYwoKpU=</DigestValue>" +
            "</Reference>" +
            "</SignedInfo>" +
            "<SignatureValue>PixoQvH7EQvaFKVUsjxZCC0tCkngY8a29XkPyS7ujw48QnMimjMLGkqq5JzBmZQ4GCQ+X4QsKGlcUG08l7b8DMPK/EOe/MMIZFnoHI1VKDGr8KLKItIOkNYLJoQ1zwQtpkLNToH6lzZN5GfjHGnyoM+jO+vNDyLQdjfeN0m04e9D13jeoBby9sxMSp0g0aW8LFpEVHMBrQGXkIIGNHCDl0o1NE0gMpUjwWklCGO4+GFbzCSprEngmcrdSvg0LpWxg7nCM7WMiSAismpe0aYcn5SK1P9087d7j8GulSH8CTTPeyD6VKCCfbn/v+OLkjtZnvzK+R8DDNA23sEnp6ApWt3ieBwfWOX+70r+KPotd09EaM7kf/8bZuFWp7VaUuA/cK9o344D3OfmH4oRI3SqkofllI0Fv9fD2wR2yrNW+siP9V+wbzLcwfoBGIncWGm37weCjXR2FLF8KsAkmISSSNICDpvsN1KgsvsTalKPqSFd7gNCWES6Y/v7Dh7HgIM8</SignatureValue>" +
            "<KeyInfo>" +
            "<X509Data>" +
            "<X509SubjectName>CN=CỤC THUẾ,O=BỘ TÀI CHÍNH,L=Hà Nội,C=VN</X509SubjectName>" +
            "<X509Certificate>MIIGWDCCBECgAwIBAgIIKgX4Emc2B7AwDQYJKoZIhvcNAQELBQAwaTELMAkGA1UEBhMCVk4xIzAhBgNVBAoMGkJhbiBDxqEgeeG6v3UgQ2jDrW5oIHBo4bunMTUwMwYDVQQDDCxDQSBwaOG7pWMgduG7pSBjw6FjIGPGoSBxdWFuIE5ow6Agbsaw4bubYyBHMjAeFw0yNTAzMTMxNjM2NTNaFw0zMDAzMTIxNjM2NTNaMFMxCzAJBgNVBAYTAlZOMRIwEAYDVQQHDAlIw6AgTuG7mWkxGTAXBgNVBAoMEELhu5ggVMOASSBDSMONTkgxFTATBgNVBAMMDEPhu6RDIFRIVeG6vjCCAaIwDQYJKoZIhvcNAQEBBQADggGPADCCAYoCggGBANHHZ3yiPk0YDoeLk2AzPpvWXyv2XsTe7s01U3rxv3C3Dvwk0eHwMqXfUHL+5eLnIiig/FPUT11QhtyI88AnpMPegAS0QiqUZztvTaCbOGfq6YHH/Dzny/+O6G8EKG0v3VgJMbEACatMH3yZrfyvIMMSHU6/2PwKJANcLrYnJENvSPRbzk5/rMVSf0PZOJcglXNRaPB9j9buya6ZVDBaObQCaNsXsn8+3W9JPHpTALUYIE1f/2TrbJpXlqlda5Z4b1if79bBoWuTCoiHnuVutuJN5bUXa6evbKjH/WjDJ7hT94E6Lek0bSWRfrOWWL6vIGBPF24u9CYdGRy1O0Fql+2FiqOJfh7ItZEoGhXvktU1dlwdHtEnfOJahW8ObOpkqfIcYGElgCvCjuBagUgfxZktLO+7JpdGqZ4kb38/ba0YCqfdxhRXGi9ll+XvcZhCYKz4XMJY68VVGlg/t005KEupmMOkPZBsg2YBpJcy/9IlRPfqNL2qH5IbSPLRe7HoHQIDAQABo4IBmDCCAZQwDAYDVR0TAQH/BAIwADAfBgNVHSMEGDAWgBRQvs+gveoPn06RKKMq63zu8C/IujBmBggrBgEFBQcBAQRaMFgwMwYIKwYBBQUHMAKGJ2h0dHA6Ly9jYS5nb3Yudm4vcGtpL3B1Yi9jcnQvY3BjYWcyLmNydDAhBggrBgEFBQcwAYYVaHR0cDovL29jc3AuY2EuZ292LnZuMBsGA1UdEQQUMBKBEGN0aHNtQGdkdC5nb3Yudm4wSgYDVR0gBEMwQTA/BghghUABAQEBATAzMDEGCCsGAQUFBwIBFiVodHRwczovL2NhLmdvdi52bi9wa2kvcHViL3BvbGljaWVzL0NQMCkGA1UdJQQiMCAGCCsGAQUFBwMCBggrBgEFBQcDBAYKKwYBBAGCNxQCAjA4BgNVHR8EMTAvMC2gK6AphidodHRwOi8vY2EuZ292LnZuL3BraS9wdWIvY3JsL2NwY2FnMi5jcmwwHQYDVR0OBBYEFCVkL5yOTVW2uxJ1T/BPHON1kWRsMA4GA1UdDwEB/wQEAwIE8DANBgkqhkiG9w0BAQsFAAOCAgEAFGUo2LNWyIpP2Yi9qXn6Uu3MBS4ZUnDso1EC64rWOPD5CTOAHeNLbcUqSlTOPZFG1pf7Qo8JRm+KSWOYjdTdoITN9c4uIxFYDo95arMIqF6pn0EPvpZHnYa2l89Rl6anc8ZsqUscj6ZqZZ8RFedBjdbMsMWGxPKpeQtJMQwvYfFQaOYO/5C/c9LDbJcc7Uhe1znfnDWQdjqGBzXTafW9lBkLMt6TI/39qsyuwiHiUPySUPaFTtOGKXc8s37ppz9gyRdMCYu6Tb1CcbOVaenZyy7XajmViAyha7AMjIC7cY1iJftjOEqVU8ZWURI9Wlm4I2Uqu/BhlO/OXe5fWpQV87vzHbWRvD29An3QDQjbQZmZ7F7Wm6QQ7hlD7w2xrhuw9cB1/d7lufwrkVlcqNypbIG5DUp9MFkao7dqg55V+CW30gWsC7WLRyGhKc7JxpGufQebs9W1p4jY1L3mNOeHwL1Gdhb9fwlRid4pFSvlJUg04EbWYOXVBAH6M8lODOM+46VGOn+HP8BKnRJWL7lWZpFcLqNAfbC3EOqgHxiahXoAqwvDr1dLA3vOW+8hFPpSa0qzwFy71nYjrJnoV3QS+R2ZNrBLTHTvPkthybscqjCxORQYcBgGcK/j5NLoFtwDHOYSK4uTi0oQNrVFhWxtfHEcGhq8lU49woJWhhXY3ws=</X509Certificate>" +
            "</X509Data>" +
            "</KeyInfo>" +
            "<Object Id=\"" + escapeXml(signingTimeId) + "\">" +
            "<SignatureProperties>" +
            "<SignatureProperty Target=\"signatureProperties\">" +
            "<SigningTime>" + escapeXml(now) + "</SigningTime>" +
            "</SignatureProperty>" +
            "</SignatureProperties>" +
            "</Object>" +
            "</Signature>" +
            "</CQT>";
        
        // Try to replace existing <CQT/> or <CQT></CQT> with the new signature
        String replaced = xml.replaceFirst("(?is)<CQT\\s*/>", java.util.regex.Matcher.quoteReplacement(cqtSignature));
        if (!replaced.equals(xml)) return replaced;
        
        replaced = xml.replaceFirst("(?is)<CQT>\\s*</CQT>", java.util.regex.Matcher.quoteReplacement(cqtSignature));
        if (!replaced.equals(xml)) return replaced;
        
        // If no CQT tag found, try to insert it in DSCKS before </DSCKS>
        replaced = xml.replaceFirst("(?is)</DSCKS>", java.util.regex.Matcher.quoteReplacement(cqtSignature) + "</DSCKS>");
        if (!replaced.equals(xml)) return replaced;
        
        // Last resort: insert before </HDon>
        replaced = xml.replaceFirst("(?is)</HDon>", java.util.regex.Matcher.quoteReplacement("<DSCKS><NBan/><NMua/>" + cqtSignature + "<CCKSKhac/></DSCKS>") + "</HDon>");
        return replaced;
    }

    // Helpers mirrored from FileController for HTML/XSLT processing and PDF generation
    private boolean looksLikeInlineXslt(String content) {
        if (content == null) return false;
        String s = content.trim();
        if (!s.startsWith("<")) return false;
        return s.contains("<xsl:stylesheet") || s.contains("xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"") || s.contains("http://www.w3.org/1999/XSL/Transform");
    }

    private Path toFilesystemPath(String publicPath) {
        if (publicPath == null || publicPath.isBlank()) return null;
        String p = publicPath.trim();
        if (p.startsWith("/")) p = p.substring(1);
        if (p.contains("..")) p = p.replace("..", "");
        Path base = java.nio.file.Paths.get(System.getProperty("user.dir"));
        return base.resolve(p);
    }

    private String resolveNamedHtmlEntities(String html) {
        if (html == null || html.isEmpty()) return html;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("&([a-zA-Z][a-zA-Z0-9]+);");
        java.util.regex.Matcher m = p.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            if ("amp".equals(name) || "lt".equals(name) || "gt".equals(name) || "quot".equals(name) || "apos".equals(name)) {
                m.appendReplacement(sb, m.group(0));
                continue;
            }
            String repl = HTML_ENTITY_TO_UNICODE.get(name);
            if (repl == null) repl = " ";
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static final java.util.Map<String, String> HTML_ENTITY_TO_UNICODE = new java.util.HashMap<>();
    static {
        // Comprehensive HTML entity map for proper Vietnamese character support
        HTML_ENTITY_TO_UNICODE.put("nbsp", "\u00A0");
        HTML_ENTITY_TO_UNICODE.put("iexcl", "\u00A1");
        HTML_ENTITY_TO_UNICODE.put("cent", "\u00A2");
        HTML_ENTITY_TO_UNICODE.put("pound", "\u00A3");
        HTML_ENTITY_TO_UNICODE.put("curren", "\u00A4");
        HTML_ENTITY_TO_UNICODE.put("yen", "\u00A5");
        HTML_ENTITY_TO_UNICODE.put("brvbar", "\u00A6");
        HTML_ENTITY_TO_UNICODE.put("sect", "\u00A7");
        HTML_ENTITY_TO_UNICODE.put("uml", "\u00A8");
        HTML_ENTITY_TO_UNICODE.put("copy", "\u00A9");
        HTML_ENTITY_TO_UNICODE.put("ordf", "\u00AA");
        HTML_ENTITY_TO_UNICODE.put("laquo", "\u00AB");
        HTML_ENTITY_TO_UNICODE.put("not", "\u00AC");
        HTML_ENTITY_TO_UNICODE.put("shy", "\u00AD");
        HTML_ENTITY_TO_UNICODE.put("reg", "\u00AE");
        HTML_ENTITY_TO_UNICODE.put("macr", "\u00AF");
        HTML_ENTITY_TO_UNICODE.put("deg", "\u00B0");
        HTML_ENTITY_TO_UNICODE.put("plusmn", "\u00B1");
        HTML_ENTITY_TO_UNICODE.put("sup2", "\u00B2");
        HTML_ENTITY_TO_UNICODE.put("sup3", "\u00B3");
        HTML_ENTITY_TO_UNICODE.put("acute", "\u00B4");
        HTML_ENTITY_TO_UNICODE.put("micro", "\u00B5");
        HTML_ENTITY_TO_UNICODE.put("para", "\u00B6");
        HTML_ENTITY_TO_UNICODE.put("middot", "\u00B7");
        HTML_ENTITY_TO_UNICODE.put("cedil", "\u00B8");
        HTML_ENTITY_TO_UNICODE.put("sup1", "\u00B9");
        HTML_ENTITY_TO_UNICODE.put("ordm", "\u00BA");
        HTML_ENTITY_TO_UNICODE.put("raquo", "\u00BB");
        HTML_ENTITY_TO_UNICODE.put("frac14", "\u00BC");
        HTML_ENTITY_TO_UNICODE.put("frac12", "\u00BD");
        HTML_ENTITY_TO_UNICODE.put("frac34", "\u00BE");
        HTML_ENTITY_TO_UNICODE.put("iquest", "\u00BF");
        // Vietnamese uppercase letters with diacritics
        HTML_ENTITY_TO_UNICODE.put("Agrave", "\u00C0");
        HTML_ENTITY_TO_UNICODE.put("Aacute", "\u00C1");
        HTML_ENTITY_TO_UNICODE.put("Acirc", "\u00C2");  // Â
        HTML_ENTITY_TO_UNICODE.put("Atilde", "\u00C3");
        HTML_ENTITY_TO_UNICODE.put("Auml", "\u00C4");
        HTML_ENTITY_TO_UNICODE.put("Aring", "\u00C5");
        HTML_ENTITY_TO_UNICODE.put("AElig", "\u00C6");
        HTML_ENTITY_TO_UNICODE.put("Ccedil", "\u00C7");
        HTML_ENTITY_TO_UNICODE.put("Egrave", "\u00C8");
        HTML_ENTITY_TO_UNICODE.put("Eacute", "\u00C9");
        HTML_ENTITY_TO_UNICODE.put("Ecirc", "\u00CA");  // Ê
        HTML_ENTITY_TO_UNICODE.put("Euml", "\u00CB");
        HTML_ENTITY_TO_UNICODE.put("Igrave", "\u00CC");
        HTML_ENTITY_TO_UNICODE.put("Iacute", "\u00CD");
        HTML_ENTITY_TO_UNICODE.put("Icirc", "\u00CE");
        HTML_ENTITY_TO_UNICODE.put("Iuml", "\u00CF");
        HTML_ENTITY_TO_UNICODE.put("ETH", "\u00D0");
        HTML_ENTITY_TO_UNICODE.put("Ntilde", "\u00D1");
        HTML_ENTITY_TO_UNICODE.put("Ograve", "\u00D2");
        HTML_ENTITY_TO_UNICODE.put("Oacute", "\u00D3");
        HTML_ENTITY_TO_UNICODE.put("Ocirc", "\u00D4");  // Ô
        HTML_ENTITY_TO_UNICODE.put("Otilde", "\u00D5");
        HTML_ENTITY_TO_UNICODE.put("Ouml", "\u00D6");
        HTML_ENTITY_TO_UNICODE.put("times", "\u00D7");
        HTML_ENTITY_TO_UNICODE.put("Oslash", "\u00D8");
        HTML_ENTITY_TO_UNICODE.put("Ugrave", "\u00D9");
        HTML_ENTITY_TO_UNICODE.put("Uacute", "\u00DA");
        HTML_ENTITY_TO_UNICODE.put("Ucirc", "\u00DB");
        HTML_ENTITY_TO_UNICODE.put("Uuml", "\u00DC");
        HTML_ENTITY_TO_UNICODE.put("Yacute", "\u00DD");
        HTML_ENTITY_TO_UNICODE.put("THORN", "\u00DE");
        HTML_ENTITY_TO_UNICODE.put("szlig", "\u00DF");
        // Vietnamese lowercase letters with diacritics
        HTML_ENTITY_TO_UNICODE.put("agrave", "\u00E0");
        HTML_ENTITY_TO_UNICODE.put("aacute", "\u00E1");
        HTML_ENTITY_TO_UNICODE.put("acirc", "\u00E2");  // â
        HTML_ENTITY_TO_UNICODE.put("atilde", "\u00E3");
        HTML_ENTITY_TO_UNICODE.put("auml", "\u00E4");
        HTML_ENTITY_TO_UNICODE.put("aring", "\u00E5");
        HTML_ENTITY_TO_UNICODE.put("aelig", "\u00E6");
        HTML_ENTITY_TO_UNICODE.put("ccedil", "\u00E7");
        HTML_ENTITY_TO_UNICODE.put("egrave", "\u00E8");
        HTML_ENTITY_TO_UNICODE.put("eacute", "\u00E9");
        HTML_ENTITY_TO_UNICODE.put("ecirc", "\u00EA");  // ê
        HTML_ENTITY_TO_UNICODE.put("euml", "\u00EB");
        HTML_ENTITY_TO_UNICODE.put("igrave", "\u00EC");
        HTML_ENTITY_TO_UNICODE.put("iacute", "\u00ED");
        HTML_ENTITY_TO_UNICODE.put("icirc", "\u00EE");  // î
        HTML_ENTITY_TO_UNICODE.put("iuml", "\u00EF");
        HTML_ENTITY_TO_UNICODE.put("eth", "\u00F0");
        HTML_ENTITY_TO_UNICODE.put("ntilde", "\u00F1");
        HTML_ENTITY_TO_UNICODE.put("ograve", "\u00F2");
        HTML_ENTITY_TO_UNICODE.put("oacute", "\u00F3");
        HTML_ENTITY_TO_UNICODE.put("ocirc", "\u00F4");  // ô
        HTML_ENTITY_TO_UNICODE.put("otilde", "\u00F5");
        HTML_ENTITY_TO_UNICODE.put("ouml", "\u00F6");
        HTML_ENTITY_TO_UNICODE.put("divide", "\u00F7");
        HTML_ENTITY_TO_UNICODE.put("oslash", "\u00F8");
        HTML_ENTITY_TO_UNICODE.put("ugrave", "\u00F9");
        HTML_ENTITY_TO_UNICODE.put("uacute", "\u00FA");
        HTML_ENTITY_TO_UNICODE.put("ucirc", "\u00FB");
        HTML_ENTITY_TO_UNICODE.put("uuml", "\u00FC");
        HTML_ENTITY_TO_UNICODE.put("yacute", "\u00FD");
        HTML_ENTITY_TO_UNICODE.put("thorn", "\u00FE");
        HTML_ENTITY_TO_UNICODE.put("yuml", "\u00FF");
        // Punctuation and symbols
        HTML_ENTITY_TO_UNICODE.put("ndash", "\u2013");
        HTML_ENTITY_TO_UNICODE.put("mdash", "\u2014");
        HTML_ENTITY_TO_UNICODE.put("lsquo", "\u2018");
        HTML_ENTITY_TO_UNICODE.put("rsquo", "\u2019");
        HTML_ENTITY_TO_UNICODE.put("ldquo", "\u201C");
        HTML_ENTITY_TO_UNICODE.put("rdquo", "\u201D");
        HTML_ENTITY_TO_UNICODE.put("hellip", "\u2026");
        HTML_ENTITY_TO_UNICODE.put("bull", "\u2022");
        HTML_ENTITY_TO_UNICODE.put("euro", "\u20AC");
        HTML_ENTITY_TO_UNICODE.put("trade", "\u2122");
    }

    private String normalizeToXhtml(String html) {
        if (html == null || html.isEmpty()) return html;
        String[] voidTags = {"meta","link","img","br","hr","input","source","track","area","base","col","embed","param","wbr"};
        for (String tag : voidTags) {
            String pattern = "(?i)<" + tag + "(\\b[^>]*?)(?<!/)>";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(html);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String attrs = m.group(1);
                String repl = "<" + tag + attrs + "/>";
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(repl));
            }
            m.appendTail(sb);
            html = sb.toString();
        }
        return html;
    }

    private String injectQrPlaceholder(String html) {
        if (html == null || html.isBlank()) return html;
        String img = "<img src=\"" + sampleQrDataUrl() + "\" alt=\"QR\" style=\"width:100%;height:100%;object-fit:contain;display:block;\"/>";
        String replacement = "<div class=\"qr-code\">" + img + "</div>";
        String safeReplacement = java.util.regex.Matcher.quoteReplacement(replacement);
        html = html.replaceAll("(?i)<div\\s+class=\\\"qr-code\\\"\\s*/>", safeReplacement);
        html = html.replaceAll("(?i)<div\\s+class=\\\"qr-code\\\"[^>]*>\\s*</div>", safeReplacement);
        return html;
    }

    private String sampleQrDataUrl() {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>" +
                "<rect width='100' height='100' fill='%23fff'/>" +
                "<rect x='8' y='8' width='24' height='24' fill='%23000'/>" +
                "<rect x='68' y='8' width='24' height='24' fill='%23000'/>" +
                "<rect x='8' y='68' width='24' height='24' fill='%23000'/>" +
                "<rect x='36' y='36' width='28' height='28' fill='%23000'/>" +
                "<text x='50' y='95' font-size='10' text-anchor='middle' fill='%23000'>QR</text>" +
                "</svg>";
        String base64 = java.util.Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "data:image/svg+xml;base64," + base64;
    }

    private String sanitizeImgSrcAttributes(String html) {
        if (html == null || html.isEmpty()) return html;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?i)(<img\\b[^>]*?\\bsrc=)([\"'])(.+?)(\\2)", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String prefix = m.group(1);
            String quote = m.group(2);
            String src = m.group(3);
            String safeSrc = src;
            if (src.indexOf('<') >= 0) {
                String lower = src.toLowerCase();
                if (lower.startsWith("data:image/svg+xml")) {
                    int comma = src.indexOf(',');
                    if (comma > 0) {
                        String svgPayload = src.substring(comma + 1);
                        String base64 = java.util.Base64.getEncoder().encodeToString(svgPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        safeSrc = "data:image/svg+xml;base64," + base64;
                    } else {
                        safeSrc = percentEncodeAngleBrackets(src);
                    }
                } else {
                    safeSrc = percentEncodeAngleBrackets(src);
                }
            }
            String replacement = prefix + quote + safeSrc + quote;
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String percentEncodeAngleBrackets(String s) {
        if (s == null) return null;
        return s.replace("<", "%3C")
                .replace(">", "%3E")
                .replace("\"", "%22")
                .replace("'", "%27")
                .replace(" ", "%20")
                .replace("#", "%23");
    }

    static class ClasspathFirstUriResolver implements FSUriResolver {
        private final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        @Override
        public String resolveURI(String baseUri, String uri) {
            if (uri == null || uri.isBlank()) return uri;
            String u = uri.trim();
            String lower = u.toLowerCase();
            if (lower.startsWith("data:")) return u;
            if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file:")) return u;
            if (lower.startsWith("classpath:")) {
                String path = u.substring("classpath:".length());
                if (path.startsWith("/")) path = path.substring(1);
                String[] candidates = new String[] { path, "static/" + path, "public/" + path, "templates/" + path };
                for (String c : candidates) {
                    java.net.URL url = cl.getResource(c);
                    if (url != null) return url.toExternalForm();
                }
                return u;
            }
            if (u.startsWith("/")) {
                String path = u.substring(1);
                String[] candidates = new String[] { "static/" + path, "public/" + path, path };
                for (String c : candidates) {
                    java.net.URL url = cl.getResource(c);
                    if (url != null) return url.toExternalForm();
                }
                try {
                    Path fs = java.nio.file.Paths.get(System.getProperty("user.dir")).resolve(path);
                    if (Files.exists(fs)) return fs.toUri().toString();
                } catch (Exception ignore) {}
                return u;
            }
            if (baseUri != null && !baseUri.isBlank()) {
                try { return java.net.URI.create(baseUri).resolve(u).toString(); } catch (Exception ignore) {}
            }
            String[] candidates = new String[] { u, "static/" + u, "public/" + u };
            for (String c : candidates) {
                java.net.URL url = cl.getResource(c);
                if (url != null) return url.toExternalForm();
            }
            try {
                Path fs = java.nio.file.Paths.get(System.getProperty("user.dir")).resolve(u);
                if (Files.exists(fs)) return fs.toUri().toString();
            } catch (Exception ignore) {}
            return u;
        }
    }

    private String ensurePdfCss(String html) {
        if (html == null || html.isBlank()) return html;
        if (html.matches("(?is).*@page\\s*\\{")) return html;
        String css = "<style>" +
                "@page { size: A4; margin: 10mm; }" +
                "html, body { print-color-adjust: exact; -webkit-print-color-adjust: exact; }" +
                "img { max-width: 100%; }" +
                "table { border-collapse: collapse; }" +
                "</style>";
        if (html.matches("(?is).*<head[^>]*>.*")) {
            return html.replaceFirst("(?is)(<head[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(css));
        }
        if (html.matches("(?is).*<html[^>]*>.*")) {
            return html.replaceFirst("(?is)(<html[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(css));
        }
        return css + html;
    }

    private String ensurePdfFontCss(String html) {
        if (html == null || html.isBlank()) return html;
        String fontCss = "<style>" +
                "@media all {" +
                "  * { font-family: 'DejaVu Sans', 'Noto Sans', 'Segoe UI', Tahoma, 'Arial Unicode MS', Arial, sans-serif !important; }" +
                "}" +
                "</style>";
        if (html.matches("(?is).*<head[^>]*>.*")) {
            html = html.replaceFirst("(?is)(<head[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(fontCss));
        } else if (html.matches("(?is).*<html[^>]*>.*")) {
            html = html.replaceFirst("(?is)(<html[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(fontCss));
        } else {
            html = fontCss + html;
        }
        return html;
    }

    private String ensurePdfLayoutFallbackCss(String html) {
        if (html == null || html.isBlank()) return html;
        String css = "<style>" +
                "@media print, all {" +
                "  .header { display: table !important; width: 100%; border-bottom: 2px solid #000; margin-bottom: 12px; }" +
                "  .header > * { display: table-cell !important; vertical-align: middle; padding-bottom: 8px; }" +
                "  .header > :nth-child(1) { width: 80px; }" +
                "  .header > :nth-child(2) { width: auto; }" +
                "  .header > :nth-child(3) { white-space: nowrap; }" +
                "  .header > * { padding-right: 10px; }" +
                "  .header > :last-child { padding-right: 0; }" +
                "  .signatures { display: table !important; width: 100%; margin-top: 20px; font-size: 11.5px; text-align: center; }" +
                "  .signatures > * { display: table-cell !important; text-align: center; vertical-align: top; }" +
                "  .buyer-line { display: table !important; width: 100%; margin-bottom: 2px; min-height: 18px; }" +
                "  .buyer-line > * { display: table-cell !important; vertical-align: middle; }" +
                "  .logo { display: block !important; position: relative !important; }" +
                "  .logo-inner { display: block !important; position: absolute !important; top: 0; bottom: 0; left: 0; right: 0; margin: auto; text-align: center; }" +
                "}" +
                "</style>";
        if (html.matches("(?is).*<head[^>]*>.*")) {
            return html.replaceFirst("(?is)(<head[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(css));
        }
        if (html.matches("(?is).*<html[^>]*>.*")) {
            return html.replaceFirst("(?is)(<html[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(css));
        }
        return css + html;
    }

    private String ensurePdfTypoCss(String html) {
        if (html == null || html.isBlank()) return html;
        String css = "<style>" +
                "@media print, all {" +
                "  html, body { -webkit-font-smoothing: antialiased; text-rendering: optimizeLegibility; font-synthesis: none; }" +
                "  b, strong { font-weight: 600 !important; }" +
                "  [style*='font-weight:700'] { font-weight: 600 !important; }" +
                "  h1, h2, h3, h4, h5 { font-weight: 600 !important; }" +
                "}" +
                "</style>";
        if (html.matches("(?is).*<head[^>]*>.*")) {
            return html.replaceFirst("(?is)(<head[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(css));
        }
        if (html.matches("(?is).*<html[^>]*>.*")) {
            return html.replaceFirst("(?is)(<html[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(css));
        }
        return css + html;
    }

    private void registerAvailableUnicodeFonts(PdfRendererBuilder builder) {
        try {
            Path dejavu = java.nio.file.Paths.get("/usr/share/fonts/truetype/dejavu");
            registerFontIfExists(builder, dejavu, "DejaVuSans.ttf", "DejaVu Sans");
            registerFontIfExists(builder, dejavu, "DejaVuSans-Bold.ttf", "DejaVu Sans");
            registerFontIfExists(builder, dejavu, "DejaVuSansCondensed.ttf", "DejaVu Sans");
            registerFontIfExists(builder, dejavu, "DejaVuSansCondensed-Bold.ttf", "DejaVu Sans");
        } catch (Throwable ignore) {}
        try {
            Path noto = java.nio.file.Paths.get("/usr/share/fonts/noto");
            registerFontIfExists(builder, noto, "NotoSans-Regular.ttf", "Noto Sans");
            registerFontIfExists(builder, noto, "NotoSans-Bold.ttf", "Noto Sans");
            registerFontIfExists(builder, noto, "NotoSansDisplay-Regular.ttf", "Noto Sans");
            registerFontIfExists(builder, noto, "NotoSansDisplay-Bold.ttf", "Noto Sans");
        } catch (Throwable ignore) {}
        String windowsFonts = System.getenv("WINDIR");
        Path fontsDir = null;
        if (windowsFonts != null && !windowsFonts.isBlank()) {
            fontsDir = java.nio.file.Paths.get(windowsFonts, "Fonts");
        } else {
            fontsDir = java.nio.file.Paths.get("C:", "Windows", "Fonts");
        }
        registerFontIfExists(builder, fontsDir, "segoeui.ttf", "Segoe UI");
        registerFontIfExists(builder, fontsDir, "segoeuib.ttf", "Segoe UI");
        registerFontIfExists(builder, fontsDir, "tahoma.ttf", "Tahoma");
        registerFontIfExists(builder, fontsDir, "tahomabd.ttf", "Tahoma");
        registerFontIfExists(builder, fontsDir, "ARIALUNI.TTF", "Arial Unicode MS");
    }

    private void registerFontIfExists(PdfRendererBuilder builder, Path fontsDir, String fileName, String family) {
        if (fontsDir == null) return;
        try {
            Path p = fontsDir.resolve(fileName);
            if (Files.exists(p)) {
                java.io.File f = p.toFile();
                builder.useFont(f, family);
            }
        } catch (Throwable ignore) {}
    }

    private String ensureUtf8Meta(String html) {
        if (html == null || html.isBlank()) return html;
        String meta = "<meta charset=\"UTF-8\"/>";
        if (html.matches("(?is).*<head[^>]*>.*")) {
            if (!html.matches("(?is).*<meta\\s+charset=\\\"?utf-8\\\"?.*")) {
                html = html.replaceFirst("(?is)(<head[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(meta));
            }
            return html;
        }
        if (html.matches("(?is).*<html[^>]*>.*")) {
            return html.replaceFirst("(?is)(<html[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement("<head>" + meta + "</head>"));
        }
        return "<head>" + meta + "</head>" + html;
    }

    private String forceReplaceFontFamilies(String html) {
        if (html == null || html.isBlank()) return html;
        String[] badFonts = new String[] { "Times New Roman", "Times", "Georgia", "Roboto", "Tahoma", "Arial", "Helvetica", "Calibri", "Cambria" };
        for (String f : badFonts) {
            String patternCss = "(?is)font-family\\s*:\\s*" + java.util.regex.Pattern.quote(f) + "(\\s*,[^;]*)?;";
            html = html.replaceAll(patternCss, "font-family: 'DejaVu Sans', 'Noto Sans', sans-serif;");
            String patternInline = "(?is)(font-family)(\\s*=\\s*)(\\\"|')" + java.util.regex.Pattern.quote(f) + "(\\3)";
            html = html.replaceAll(patternInline, "$1$2$3DejaVu Sans$3");
        }
        html = html.replaceAll("(?is)<font\\s+face=\\\"[^\\\"]+\\\"", "<font face=\"DejaVu Sans\"");
        return html;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @PostMapping("/{id}/send-to-cqt")
    @Transactional
    public ResponseEntity<?> sendToCqt(@PathVariable("id") Long id, @AuthenticationPrincipal UserEntity user) {
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
        SignatureVatEntity sig = signatureVatRepository.findTopByInvoiceIdOrderByIdDesc(inv.getId().intValue()).orElse(null);
        if (sig == null || sig.getXml() == null || sig.getXml().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Hóa đơn chưa được ký số"));
        }
        // Determine message code based on form.haveCode
        Integer messageType = 200; String description;
        FormInvoiceEntity form = null;
        try {
            Long formId = inv.getFormId() != null ? inv.getFormId().longValue() : null;
            if (formId != null) form = formInvoiceRepository.findById(formId).orElse(null);
        } catch (Exception ignore) {}
        Integer have = form != null ? form.getHaveCode() : null;
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
        h.setXmlData(sig.getXml());
        try { historyService.save(h); } catch (Exception e) { log.warn("History save failed for invoice {}: {}", id, e.toString()); }

        // Update invoice status to 2 (đã gửi thuế)
        inv.setStatus((short)2);
        inv.setUpdatedAt(java.time.LocalDateTime.now());
        invoiceRepository.save(inv);

        // Simulate asynchronous CQT response (202 or 204) - use a separate method with proper transaction
        Long finalInvoiceId = inv.getId();
        Long finalCompanyId = uCompany;
        Integer finalHave = have;
        new Thread(() -> {
            try {
                Thread.sleep(1500L);
                // Use self-injection to ensure @Transactional works
                if (self != null) {
                    self.processCqtResponse(finalInvoiceId, finalCompanyId, finalHave);
                } else {
                    log.error("Self-injection failed, cannot process CQT response with transaction");
                }
            } catch (InterruptedException e) {
                log.warn("CQT response thread interrupted: {}", e.toString());
            }
        }).start();

        java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("id", inv.getId());
        resp.put("status", 2);
        return ResponseEntity.ok(resp);
    }

    // Process CQT response with proper transaction handling
    @Transactional
    public void processCqtResponse(Long invoiceId, Long companyId, Integer have) {
        try {
            InvoiceEntity inv = invoiceRepository.findById(invoiceId).orElse(null);
            if (inv == null) {
                log.warn("processCqtResponse: Invoice {} not found", invoiceId);
                return;
            }
            
            // Ensure we have the latest tax upload xml
            SignatureAuthoritiesTaxDTO lastTax = null;
            try {
                lastTax = signatureAuthoritiesTaxService.getLatestByInvoiceId(inv.getId().intValue());
            } catch (Exception ignore) {}
            String xml = lastTax != null ? lastTax.xml : null;
            if (xml == null || xml.isBlank()) {
                SignatureVatEntity sig = signatureVatRepository.findTopByInvoiceIdOrderByIdDesc(inv.getId().intValue()).orElse(null);
                xml = sig != null ? sig.getXml() : null;
            }
            if (xml == null || xml.isBlank()) {
                log.warn("processCqtResponse: No XML found for invoice {}", invoiceId);
                return;
            }
            
            boolean isCapMa = have != null && have == 1;

            if (isCapMa) {
                // Message 200: either accepted (202) or rejected (204)
                boolean accepted = true; // Always accept in simulation
                if (accepted) {
                    // 202: Chấp nhận, generate MCCQT and update invoice
                    String code = generateCqtCode(34);
                    String xmlWithCode = insertMccqt(xml, code);
                    
                    // Add CQT signature to the XML (simulated)
                    String xmlWithCqtSignature = injectCqtSignature(xmlWithCode);
                    
                    // Persist updated xml with CQT signature to signature_authorities_tax
                    try {
                        SignatureAuthoritiesTaxDTO a = new SignatureAuthoritiesTaxDTO();
                        a.companyId = companyId != null ? companyId.intValue() : null;
                        a.invoiceId = inv.getId().intValue();
                        a.xml = xmlWithCqtSignature;
                        signatureAuthoritiesTaxService.create(a);
                    } catch (Exception e) { 
                        log.error("Persist CQT xml failed for invoice {}: {}", invoiceId, e.toString()); 
                    }
                    
                    // Update invoice code_cqt and status = 3 (Đã phát hành)
                    try { inv.setCodeCqt(code); } catch (Exception ignore) {}
                    inv.setStatus((short)3);
                    inv.setUpdatedAt(java.time.LocalDateTime.now());
                    invoiceRepository.save(inv);
                    
                    // History type=202, user_id=0
                    HistoryDto h202 = new HistoryDto();
                    h202.setCompanyId(companyId);
                    h202.setUserId(0L);
                    h202.setTableName("invoices");
                    h202.setTableId(inv.getId().longValue());
                    h202.setTitle("Hóa đơn số " + inv.getNo() + " đã được cấp mã từ CQT");
                    h202.setDescription("Mã CQT: " + code);
                    h202.setShowNotify(1);
                    h202.setStatus(1);
                    h202.setType(202);
                    h202.setXmlData(xmlWithCqtSignature);
                    
                    try { 
                        HistoryDto saved = historyService.save(h202);
                        if (saved == null || saved.getId() == null) {
                            log.error("History 202 save returned null for invoice {}", invoiceId);
                        } else {
                            log.info("History 202 saved successfully with ID {} for invoice {}", saved.getId(), invoiceId);
                        }
                    } catch (Exception e) { 
                        log.error("History 202 save failed for invoice {}: {}", invoiceId, e.toString(), e); 
                    }
                } else {
                    // 204: Từ chối cấp mã
                    inv.setStatus((short)7); // Không đủ điều kiện
                    inv.setUpdatedAt(java.time.LocalDateTime.now());
                    invoiceRepository.save(inv);
                    
                    HistoryDto h204rej = new HistoryDto();
                    h204rej.setCompanyId(companyId);
                    h204rej.setUserId(0L);
                    h204rej.setTableName("invoices");
                    h204rej.setTableId(inv.getId().longValue());
                    h204rej.setTitle("Hóa đơn số " + inv.getNo() + " bị từ chối cấp mã từ CQT");
                    h204rej.setDescription("Hóa đơn không đủ điều kiện");
                    h204rej.setShowNotify(1);
                    h204rej.setStatus(1);
                    h204rej.setType(204);
                    h204rej.setXmlData(buildMock204ResponseXml(false, inv, null));
                    
                    try { 
                        historyService.save(h204rej);
                        log.info("History 204 reject saved for invoice {}", invoiceId);
                    } catch (Exception e) { 
                        log.error("History 204 reject save failed for invoice {}: {}", invoiceId, e.toString(), e); 
                    }
                }
            } else {
                // Message 203: always respond 204; LTBao determines acceptance
                boolean ltBao2 = true; // true => accepted (LTBao==2)
                if (ltBao2) {
                    // Add CQT signature to the XML (simulated)
                    String xmlWithCqtSignature = injectCqtSignature(xml);
                    
                    // Persist updated xml with CQT signature to signature_authorities_tax
                    try {
                        SignatureAuthoritiesTaxDTO a = new SignatureAuthoritiesTaxDTO();
                        a.companyId = companyId != null ? companyId.intValue() : null;
                        a.invoiceId = inv.getId().intValue();
                        a.xml = xmlWithCqtSignature;
                        signatureAuthoritiesTaxService.create(a);
                    } catch (Exception e) { 
                        log.error("Persist CQT xml with signature failed for invoice {}: {}", invoiceId, e.toString()); 
                    }
                    
                    inv.setStatus((short)3); // Tiếp nhận/Chấp nhận
                    inv.setUpdatedAt(java.time.LocalDateTime.now());
                    invoiceRepository.save(inv);
                    
                    HistoryDto h204acc = new HistoryDto();
                    h204acc.setCompanyId(companyId);
                    h204acc.setUserId(0L);
                    h204acc.setTableName("invoices");
                    h204acc.setTableId(inv.getId().longValue());
                    h204acc.setTitle("Hóa đơn số " + inv.getNo() + " đã được chấp nhận từ CQT");
                    h204acc.setDescription("Thông báo 204: Chấp nhận");
                    h204acc.setShowNotify(1);
                    h204acc.setStatus(1);
                    h204acc.setType(204);
                    h204acc.setXmlData(buildMock204ResponseXml(true, inv, null)); // LTBao==2
                    
                    try { 
                        HistoryDto saved = historyService.save(h204acc);
                        if (saved == null || saved.getId() == null) {
                            log.error("History 204 accept save returned null for invoice {}", invoiceId);
                        } else {
                            log.info("History 204 accept saved successfully with ID {} for invoice {}", saved.getId(), invoiceId);
                        }
                    } catch (Exception e) { 
                        log.error("History 204 accept save failed for invoice {}: {}", invoiceId, e.toString(), e); 
                    }
                } else {
                    inv.setStatus((short)7); // Không đủ điều kiện
                    inv.setUpdatedAt(java.time.LocalDateTime.now());
                    invoiceRepository.save(inv);
                    
                    HistoryDto h204rej = new HistoryDto();
                    h204rej.setCompanyId(companyId);
                    h204rej.setUserId(0L);
                    h204rej.setTableName("invoices");
                    h204rej.setTableId(inv.getId().longValue());
                    h204rej.setTitle("Hóa đơn số " + inv.getNo() + " bị từ chối từ CQT");
                    h204rej.setDescription("Hóa đơn không đủ điều kiện");
                    h204rej.setShowNotify(1);
                    h204rej.setStatus(1);
                    h204rej.setType(204);
                    h204rej.setXmlData(buildMock204ResponseXml(false, inv, null)); // LTBao!=2
                    
                    try { 
                        historyService.save(h204rej);
                        log.info("History 204 reject saved for invoice {}", invoiceId);
                    } catch (Exception e) { 
                        log.error("History 204 reject save failed for invoice {}: {}", invoiceId, e.toString(), e); 
                    }
                }
            }
        } catch (Exception e) {
            log.error("processCqtResponse failed for invoice {}: {}", invoiceId, e.toString(), e);
        }
    }

    // Build a minimal 204 response payload with DLieu->TBao->DLTBao->LTBao
    private String buildMock204ResponseXml(boolean accepted, InvoiceEntity inv, String extra) {
        String ltBao = accepted ? "2" : "1"; // 2: accepted, others: rejected
        String id = inv != null ? String.valueOf(inv.getId()) : "";
        String no = inv != null && inv.getNo() != null ? String.valueOf(inv.getNo()) : "";
        String codeCqt = inv != null ? safeStr(inv.getCodeCqt()) : "";
        String now = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String note = extra != null ? extra : (accepted ? "TBao: Chấp nhận" : "TBao: Từ chối");
        return "<DLieu>" +
                "<TBao>" +
                "<TTChung>" +
                "<TGian>" + escapeXml(now) + "</TGian>" +
                "<IdHD>" + escapeXml(id) + "</IdHD>" +
                "<SoHD>" + escapeXml(no) + "</SoHD>" +
                (codeCqt.isEmpty() ? "" : ("<MCCQT>" + escapeXml(codeCqt) + "</MCCQT>")) +
                "</TTChung>" +
                "<DLTBao>" +
                "<LTBao>" + ltBao + "</LTBao>" +
                "<GChu>" + escapeXml(note) + "</GChu>" +
                "</DLTBao>" +
                "</TBao>" +
                "</DLieu>";
    }

    // Generate random uppercase alphanumeric CQT code of given length
    private String generateCqtCode(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(len);
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    // Insert or replace <MCCQT> inside <DLHDon> in the signed XML
    private String insertMccqt(String xml, String code) {
        if (xml == null || xml.isBlank()) return xml;
        String safeCode = escapeXml(code);
        // If MCCQT exists, replace its content
        String replaced = xml.replaceFirst("(?is)(<MCCQT>)(.*?)(</MCCQT>)", "$1" + java.util.regex.Matcher.quoteReplacement(safeCode) + "$3");
        if (!replaced.equals(xml)) return replaced;
        // Else insert <MCCQT> before closing </HDon>
        return xml.replaceFirst("(?is)</HDon>", "<MCCQT>" + java.util.regex.Matcher.quoteReplacement(safeCode) + "</MCCQT></HDon>");
    }

    private String safeStr(Object o) { return o == null ? "" : String.valueOf(o); }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> history(@PathVariable("id") Long id, @AuthenticationPrincipal UserEntity user) {
        if (id == null) return ResponseEntity.badRequest().body(new ErrorDTO("Thiếu ID hóa đơn"));
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(403).build();
        }
        Optional<InvoiceEntity> opt = invoiceRepository.findById(id);
        InvoiceEntity inv = opt.orElse(null);
        if (inv == null) return ResponseEntity.status(404).body(new ErrorDTO("Không tìm thấy hóa đơn"));
        
        // Use HistoryService for efficient querying
        if (historyService == null) {
            // Fallback to old implementation if service not available
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
        
        // Use HistoryService
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
        // Only allow sending for issued invoices
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
            String lookup = safeStr(inv.getLookupCode());
            String pdfLink = "/v1/invoices/" + lookup + "/download-pdf";
            String xmlLink = "/v1/invoices/" + lookup + "/download-xml";
            String html = buildEmailHtml(inv, formSerial, pdfLink, xmlLink, name);

            if (mailSender != null) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(email);
                // If system email configured, sender will be set by JavaMail; otherwise rely on defaults
                helper.setSubject(subject);
                helper.setText(html, true);
                mailSender.send(message);
            } else {
                log.info("[DEV] Mail sender not configured. Would send to {} with subject '{}'", email, subject);
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

    /**
     * Pick invoice XML source by status:
     * - status = 0: generate XML from InvoiceXmlBuilder
     * - status = 1 or 2: use signature_vats.xml (signed XML)
     * - status > 2: use signature_authorities_tax.xml, fallback signature_vats.xml
     */
    private String getInvoiceXmlByStatus(InvoiceEntity inv, FormInvoiceEntity form, CompanyEntity company, CompanyBankEntity bank) {
        if (inv == null) return "";

        short status = inv.getStatus() == null ? 0 : inv.getStatus();
        Integer invoiceId = inv.getId() == null ? null : inv.getId().intValue();

        // 0: build from current data
        if (status == 0) {
            return vn.hoadon.util.InvoiceXmlBuilder.build(inv, form, company, bank);
        }

        // 1-2: signed XML
        if (status == 1 || status == 2) {
            String xml = getXmlFromSignatureVats(invoiceId);
            if (xml != null && !xml.isBlank()) return xml;
            // Fallback
            return vn.hoadon.util.InvoiceXmlBuilder.build(inv, form, company, bank);
        }

        // >2: CQT XML preferred
        String xmlCqt = getXmlFromSignatureAuthoritiesTax(invoiceId);
        if (xmlCqt != null && !xmlCqt.isBlank()) return xmlCqt;

        String xmlSigned = getXmlFromSignatureVats(invoiceId);
        if (xmlSigned != null && !xmlSigned.isBlank()) return xmlSigned;

        return vn.hoadon.util.InvoiceXmlBuilder.build(inv, form, company, bank);
    }

    private String getXmlFromSignatureVats(Integer invoiceId) {
        if (invoiceId == null) return null;
        try {
            java.util.List<vn.hoadon.entity.SignatureVatEntity> rows = signatureVatRepository.findByInvoiceId(invoiceId);
            if (rows == null || rows.isEmpty()) return null;
            // Try newest first if ordering is not guaranteed
            rows.sort((a, b) -> {
                java.time.LocalDateTime ca = a != null ? a.getCreatedAt() : null;
                java.time.LocalDateTime cb = b != null ? b.getCreatedAt() : null;
                if (ca == null && cb == null) return 0;
                if (ca == null) return 1;
                if (cb == null) return -1;
                return cb.compareTo(ca);
            });
            String xml = rows.get(0) != null ? rows.get(0).getXml() : null;
            return (xml != null && !xml.isBlank()) ? xml : null;
        } catch (Exception e) {
            log.warn("Failed to load signature_vats xml for invoice {}: {}", invoiceId, e.toString());
            return null;
        }
    }

    private String getXmlFromSignatureAuthoritiesTax(Integer invoiceId) {
        if (invoiceId == null) return null;
        try {
            // Prefer 'latest' if service supports it
            vn.hoadon.dto.SignatureAuthoritiesTaxDTO latest = null;
            try {
                latest = signatureAuthoritiesTaxService.getLatestByInvoiceId(invoiceId);
            } catch (Exception ignore) {}
            if (latest != null && latest.xml != null && !latest.xml.isBlank()) return latest.xml;

            java.util.List<vn.hoadon.dto.SignatureAuthoritiesTaxDTO> rows = signatureAuthoritiesTaxService.listByInvoiceId(invoiceId);
            if (rows == null || rows.isEmpty()) return null;
            rows.sort((a, b) -> {
                java.time.LocalDateTime ca = a != null ? a.createdAt : null;
                java.time.LocalDateTime cb = b != null ? b.createdAt : null;
                if (ca == null && cb == null) return 0;
                if (ca == null) return 1;
                if (cb == null) return -1;
                return cb.compareTo(ca);
            });
            String xml = rows.get(0) != null ? rows.get(0).xml : null;
            return (xml != null && !xml.isBlank()) ? xml : null;
        } catch (Exception e) {
            log.warn("Failed to load signature_authorities_tax xml for invoice {}: {}", invoiceId, e.toString());
            return null;
        }
    }
}
