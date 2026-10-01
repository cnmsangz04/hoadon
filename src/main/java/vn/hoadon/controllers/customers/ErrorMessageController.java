package vn.hoadon.controllers.customers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.errormessage.ErrorMessageDto;
import vn.hoadon.dto.errormessage.ErrorMessageUpsertRequest;
import vn.hoadon.entity.*;
import vn.hoadon.repositories.*;
import vn.hoadon.services.ErrorMessageService;
import vn.hoadon.util.ErrorMessageXmlBuilder;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/error-messages")
public class ErrorMessageController extends BaseController {
    private final ErrorMessageService service;
    private final InvoiceRepository invoiceRepository;
    private final FormInvoiceRepository formInvoiceRepository;
    private final CompanyRepository companyRepository;
    private final HashInvoiceRepository hashInvoiceRepository;
    private final HistoryRepository historyRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public ErrorMessageController(ErrorMessageService service,
                                  InvoiceRepository invoiceRepository,
                                  FormInvoiceRepository formInvoiceRepository,
                                  CompanyRepository companyRepository,
                                  HashInvoiceRepository hashInvoiceRepository,
                                  HistoryRepository historyRepository) {
        this.service = service;
        this.invoiceRepository = invoiceRepository;
        this.formInvoiceRepository = formInvoiceRepository;
        this.companyRepository = companyRepository;
        this.hashInvoiceRepository = hashInvoiceRepository;
        this.historyRepository = historyRepository;
    }

    @GetMapping("/prefill")
    public ResponseEntity<Map<String, Object>> prefill() {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        CompanyEntity company = companyRepository.findById(user.getCompanyId()).orElse(null);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("formPattern", "04/SS-HĐĐT");
        res.put("noticeDate", LocalDate.now());
        if (company != null) {
            res.put("taxpayerName", company.getName());
            res.put("taxCode", company.getTaxcode());
            TaxAuthorityEntity tax = company.getTaxAuthority() != null ? company.getTaxAuthority() : company.getTaxAuthorityCity();
            res.put("taxAuthorityName", tax != null ? tax.getName() : "");
            res.put("taxAuthorityCode", tax != null && tax.getCode() != null ? String.valueOf(tax.getCode()) : "");
        }
        return ResponseEntity.ok(res);
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(required = false) Integer notificationType,
                                    @RequestParam(required = false) String dateFrom,
                                    @RequestParam(required = false) String dateTo,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        permission("invoice-list");
        Long companyId = requireCompanyId();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ErrorMessageEntity> p = service.search(companyId, keyword, status, notificationType, parseDate(dateFrom), parseDate(dateTo), pageable);
        Page<ErrorMessageDto> dtoPage = p.map(service::toDto);
        return toPage(dtoPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ErrorMessageDto> get(@PathVariable Long id) {
        permission("invoice-list");
        return service.findByIdAndCompany(id, requireCompanyId())
                .map(service::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ErrorMessageDto> create(@RequestBody ErrorMessageUpsertRequest req) {
        permission("invoice-save");
        UserEntity user = currentUser();
        ErrorMessageEntity created = service.create(requireCompanyId(), user.getId(), req);
        return ResponseEntity.created(URI.create("/v1/error-messages/" + created.getId())).body(service.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ErrorMessageDto> update(@PathVariable Long id, @RequestBody ErrorMessageUpsertRequest req) {
        permission("invoice-save");
        return service.update(requireCompanyId(), id, req)
                .map(service::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permission("invoice-save");
        service.delete(requireCompanyId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sign-token/prepare")
    public ResponseEntity<Map<String, Object>> prepareUsbTokenSign(@PathVariable Long id) {
        permission("invoice-save");
        UserEntity user = currentUser();
        ErrorMessageEntity entity = service.findByIdAndCompany(id, requireCompanyId()).orElse(null);
        if (entity == null) return ResponseEntity.notFound().build();
        if (entity.getStatus() != null && entity.getStatus() != 0) {
            return ResponseEntity.badRequest().body(Map.<String, Object>of("message", "Chỉ được ký thông báo mới khởi tạo"));
        }
        HashInvoiceEntity hash = new HashInvoiceEntity();
        hash.setCompanyId(entity.getCompanyId());
        hash.setUserId(user.getId());
        hash.setNameUser(user.getUsername());
        hash.setHash(createHash(entity.getCompanyId()));
        hash.setIds(List.of(entity.getId()));
        hash.setIdAttr(entity.getIdAttr());
        hash.setSignBy("NNT");
        hash.setType("TBSS");
        hashInvoiceRepository.save(hash);
        return ResponseEntity.ok(Map.<String, Object>of("hash", hash.getHash(), "signBy", "NNT"));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<ErrorMessageDto> send(@PathVariable Long id) {
        permission("invoice-send|invoice-save");
        UserEntity user = currentUser();
        Optional<ErrorMessageEntity> sent = service.send(requireCompanyId(), user.getId(), id);
        return sent
                .map(entity -> ResponseEntity.accepted().body(service.toDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{id}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> unsignedXml(@PathVariable Long id) {
        permission("invoice-list");
        return service.findByIdAndCompany(id, requireCompanyId())
                .map(service::buildUnsignedXml)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{id}/download-xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> downloadXml(@PathVariable Long id) {
        permission("invoice-list");
        Optional<ErrorMessageEntity> entity = service.findByIdAndCompany(id, requireCompanyId());
        if (entity.isEmpty()) return ResponseEntity.notFound().build();
        String xml = service.getXmlForDownload(id).orElse("");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=thong-bao-sai-sot-" + id + ".xml")
                .body(xml);
    }

    @GetMapping(value = "/{id}/view", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> viewHtml(@PathVariable Long id) {
        permission("invoice-list");
        return service.findByIdAndCompany(id, requireCompanyId())
                .map(ErrorMessageXmlBuilder::buildHtml)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{id}/download-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        permission("invoice-list");
        ErrorMessageEntity entity = service.findByIdAndCompany(id, requireCompanyId()).orElse(null);
        if (entity == null) return ResponseEntity.notFound().build();
        byte[] pdf = renderPdf(ErrorMessageXmlBuilder.buildHtml(entity));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=thong-bao-sai-sot-" + id + ".pdf")
                .body(pdf);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<Map<String, Object>>> history(@PathVariable Long id) {
        permission("invoice-list");
        ErrorMessageEntity entity = service.findByIdAndCompany(id, requireCompanyId()).orElse(null);
        if (entity == null) return ResponseEntity.notFound().build();
        List<Map<String, Object>> rows = historyRepository
                .findByCompanyIdAndTableNameAndTableIdAndStatusOrderByCreatedAtDescIdDesc(entity.getCompanyId(), "error_messages", id, 1)
                .stream()
                .map(h -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", h.getId());
                    row.put("title", h.getTitle());
                    row.put("description", h.getDescription());
                    row.put("type", h.getType());
                    row.put("createdAt", h.getCreatedAt());
                    row.put("userId", h.getUserId());
                    return row;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/invoices/search")
    public Map<String, Object> searchInvoices(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String dateFrom,
                                              @RequestParam(required = false) String dateTo,
                                              @RequestParam(required = false) Integer no,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        permission("invoice-list");
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size), Sort.by(Sort.Direction.DESC, "dateExport"));
        Page<InvoiceEntity> p = invoiceRepository.searchReleasedForErrorMessage(requireCompanyId(), keyword, parseDate(dateFrom), parseDate(dateTo), no, pageable);
        Page<Map<String, Object>> mapped = p.map(this::invoiceRow);
        return toPage(mapped);
    }

    @PostMapping("/invoices/by-ids")
    public ResponseEntity<List<Map<String, Object>>> invoicesByIds(@RequestBody Map<String, Object> body) {
        permission("invoice-list");
        Long companyId = requireCompanyId();
        List<Long> ids = normalizeIds(body != null ? body.get("ids") : null);
        if (ids.isEmpty()) return ResponseEntity.ok(Collections.emptyList());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Long id : ids) {
            InvoiceEntity invoice = invoiceRepository.findById(id).orElse(null);
            if (invoice == null || invoice.getCompanyId() == null || !companyId.equals(invoice.getCompanyId().longValue())) continue;
            if (invoice.getStatus() == null || invoice.getStatus() <= 2) continue;
            rows.add(invoiceRow(invoice));
        }
        return ResponseEntity.ok(rows);
    }

    private Map<String, Object> invoiceRow(InvoiceEntity invoice) {
        Map<String, Object> row = new LinkedHashMap<>();
        FormInvoiceEntity form = invoice.getFormId() != null ? formInvoiceRepository.findById(invoice.getFormId().longValue()).orElse(null) : null;
        row.put("id", invoice.getId());
        row.put("taxCode", invoice.getCodeCqt());
        row.put("formSymbol", form != null ? form.getFormCode() : "");
        row.put("serial", form != null ? form.getSerial() : "");
        row.put("invoiceNo", invoice.getNo() != null ? String.valueOf(invoice.getNo()) : "");
        row.put("invoiceDate", invoice.getDateExport());
        row.put("invoiceType", 1);
        row.put("errorType", defaultErrorType(invoice));
        row.put("reason", "");
        row.put("customerName", customerName(invoice.getCustomer()));
        row.put("amount", invoice.getAmount());
        return row;
    }

    private Integer defaultErrorType(InvoiceEntity invoice) {
        Short status = invoice.getStatus();
        if (status != null && status == 6) return 1;
        if (status != null && status == 5) return 2;
        if (status != null && status == 4) return 3;
        return 0;
    }

    private String customerName(String customerJson) {
        if (customerJson == null || customerJson.isBlank()) return "";
        try {
            JsonNode node = mapper.readTree(customerJson);
            for (String key : List.of("cus_name", "cus_buyer", "staff_name", "name")) {
                JsonNode v = node.get(key);
                if (v != null && !v.asText("").isBlank()) return v.asText();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private List<Long> normalizeIds(Object raw) {
        if (raw == null) return List.of();
        List<Long> out = new ArrayList<>();
        if (raw instanceof Collection<?>) {
            Collection<?> items = (Collection<?>) raw;
            for (Object item : items) {
                Long id = parseLong(item);
                if (id != null && id > 0 && !out.contains(id)) out.add(id);
            }
            return out;
        }
        String text = String.valueOf(raw);
        for (String part : text.split("[,_\\s]+")) {
            Long id = parseLong(part);
            if (id != null && id > 0 && !out.contains(id)) out.add(id);
        }
        return out;
    }

    private Long parseLong(Object value) {
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Long requireCompanyId() {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) throw new org.springframework.security.access.AccessDeniedException("Không xác định được công ty");
        return user.getCompanyId();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try { return LocalDate.parse(value); } catch (Exception e) { return null; }
    }

    private Map<String, Object> toPage(Page<?> p) {
        Map<String, Object> res = new LinkedHashMap<>();
        long total = p.getTotalElements();
        int size = p.getSize();
        int currentPage = p.getNumber() + 1;
        int lastPage = Math.max(1, p.getTotalPages());
        int count = p.getNumberOfElements();
        long from = total == 0 ? 0 : ((long) (currentPage - 1) * size) + 1;
        res.put("data", p.getContent());
        res.put("total", total);
        res.put("per_page", size);
        res.put("current_page", currentPage);
        res.put("last_page", lastPage);
        res.put("from", from);
        res.put("to", total == 0 ? 0 : from + count - 1);
        res.put("prev_page_url", currentPage > 1 ? currentPage - 1 : null);
        res.put("next_page_url", currentPage < lastPage ? currentPage + 1 : null);
        return res;
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo PDF", e);
        }
    }

    private String createHash(Long companyId) {
        String raw = "PA@Hash:tbss:com" + companyId + "," + System.nanoTime() + "," + UUID.randomUUID();
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
