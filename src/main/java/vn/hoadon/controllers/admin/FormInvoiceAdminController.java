package vn.hoadon.controllers.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.invoice.template.service.InvoiceDocumentBridge;
import vn.hoadon.invoice.template.service.InvoiceFormTemplateService;
import vn.hoadon.invoice.template.service.InvoiceTemplateService;
import vn.hoadon.invoice.template.service.TemplateAccess;
import vn.hoadon.invoice.template.service.TemplateException;
import vn.hoadon.services.FormInvoiceService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/v1/administrator/form-invoices")
public class FormInvoiceAdminController extends BaseController {

    private final FormInvoiceService service;
    private final TransactionTemplate transactionTemplate;
    @Autowired private InvoiceDocumentBridge documents;
    @Autowired private InvoiceFormTemplateService formTemplates;
    @Autowired private InvoiceTemplateService templates;
    @Autowired private ObjectMapper mapper;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FormInvoiceAdminController.class);

    @Autowired
    public FormInvoiceAdminController(FormInvoiceService service, PlatformTransactionManager transactionManager) {
        this.service = service;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

   @PostMapping("/list")
public ResponseEntity<?> list(@RequestBody Map<String, Object> body,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size) {
    permission("form-invoice-manage");

    var result = service.pageByCompanySystemWithFilters(null, 1, java.util.Objects.toString(body.get("q"), ""), getInt(body, "category"), getInt(body, "type"), getInt(body, "status"), PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.DESC, "updatedAt")));
    return ResponseEntity.ok(Map.of("items", result.getContent(), "total", result.getTotalElements(), "per_page", result.getSize(), "current_page", result.getNumber() + 1, "last_page", Math.max(1, result.getTotalPages())));
}

private Integer getInt(Map<String, Object> body, String key) {
    Object val = body.get(key);
    if (val == null) return null;
    if (val instanceof Number) return ((Number) val).intValue();
    try { return Integer.valueOf(val.toString()); } catch (Exception e) { return null; }
}

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        permission("form-invoice-manage");
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        if (!Integer.valueOf(1).equals(e.getSystem())) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(e);
    }

    @GetMapping(value = "/{id}/preview", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> preview(@PathVariable Long id) {
        permission("form-invoice-manage");
        var user = currentUser();
        var access = new TemplateAccess(user.getCompanyId(), user.getId(), true);
        return documents.pdf(documents.sample(id, true, access), false, "invoice-" + id + ".pdf");
    }

    @GetMapping(value = "/{id}/view", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> viewHtml(@PathVariable Long id) {
        permission("form-invoice-manage");
        var user = currentUser();
        var access = new TemplateAccess(user.getCompanyId(), user.getId(), true);
        return documents.sampleHtml(id, true, access);
    }

    @GetMapping(value = "/{id}/download-pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> downloadPdf(@PathVariable Long id, @RequestParam(required = false) Long artifactId) {
        permission("form-invoice-manage");
        var user = currentUser();
        var access = new TemplateAccess(user.getCompanyId(), user.getId(), true);
        var artifact = documents.sampleArtifact(id, true, access, artifactId);
        return documents.pdf(artifact, true, "invoice-" + id + ".pdf");
    }

    @GetMapping(value = "/{id}/download-xml", produces = org.springframework.http.MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> downloadXml(@PathVariable Long id) {
        permission("form-invoice-manage");
        var user = currentUser();
        var access = new TemplateAccess(user.getCompanyId(), user.getId(), true);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_XML)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename("invoice-" + id + ".xml").build().toString())
                .body(documents.sampleXml(id, true, access));
    }

    @PostMapping(value = "/saveOrUpdate")
    public ResponseEntity<?> saveOrUpdate(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "category", required = false) Integer category,
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "have_code", required = false) Integer haveCode,
            @RequestParam(value = "layoutJson", required = false) MultipartFile layoutJson
    ) {
        permission("form-invoice-manage");
        log.debug("saveOrUpdate called: id={}, name={}, category={}, type={}, status={}, haveCode={}", id, name, category, type, status, haveCode);

        try {
            JsonNode layout = layoutJson == null ? null : parseLayoutJson(layoutJson);
            return transactionTemplate.execute(txStatus -> saveOrUpdateInTransaction(id, name, category, type, status, haveCode, layout));
        } catch (TemplateException ex) {
            log.warn("Invalid invoice template JSON: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage(), "code", ex.getCode()));
        } catch (IllegalArgumentException iae) {
            log.warn("Validation error: {}", iae.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception ex) {
            log.error("Error saving form invoice", ex);
            return ResponseEntity.status(500).body(Map.of("message", ex.getClass().getSimpleName() + ": " + (ex.getMessage() != null ? ex.getMessage() : "Unexpected error")));
        }
    }

    private ResponseEntity<?> saveOrUpdateInTransaction(Long id, String name, Integer category, Integer type,
                                                         Integer status, Integer haveCode, JsonNode layout) {
        FormInvoiceEntity e;
        boolean creating = (id == null);
        var user = currentUser();
        if (user == null) return ResponseEntity.status(403).build();
        if (creating) {
            e = new FormInvoiceEntity();
            e.setUserId(user.getId());
            e.setCreatedAt(LocalDateTime.now());
        } else {
            Optional<FormInvoiceEntity> opt = service.findById(id);
            if (opt.isEmpty()) return ResponseEntity.notFound().build();
            if (!Integer.valueOf(1).equals(opt.get().getSystem())) return ResponseEntity.notFound().build();
            e = new FormInvoiceEntity();
            e.setId(id);
            e.setCompanyId(opt.get().getCompanyId());
        }

        if (!StringUtils.hasText(name)) return ResponseEntity.badRequest().body(Map.of("message", "Name required"));
        e.setName(name);
        e.setCategory(category != null ? category : 1);
        e.setType(type != null ? type : 1);
        boolean activateAfterCreate = creating && Integer.valueOf(1).equals(status);
        e.setStatus(creating ? 0 : (status != null ? status : 0));
        e.setHaveCode(haveCode != null ? haveCode : 1);

        if (creating) e.setCompanyId(null);
        e.setSystem(1);
        e.setFormCode("X");
        e.setSerial("XXXXXX");
        e.setUpdatedAt(LocalDateTime.now());

        FormInvoiceEntity saved = creating
                ? service.create(e, layout)
                : service.update(id, e).orElseThrow(() -> new RuntimeException("Unable to update template"));
        if (layout != null && !creating) applyLayoutJson(saved, layout);
        if (activateAfterCreate) {
            var access = new TemplateAccess(user.getCompanyId(), user.getId(), true);
            formTemplates.saveInitialVersion(saved.getId(), access);
            FormInvoiceEntity activation = new FormInvoiceEntity();
            activation.setStatus(1);
            saved = service.update(saved.getId(), activation)
                    .orElseThrow(() -> new RuntimeException("Unable to activate template"));
        }
        return ResponseEntity.ok(saved);
    }

    private JsonNode parseLayoutJson(MultipartFile layoutJson) {
        String originalName = layoutJson.getOriginalFilename();
        if (originalName != null && !originalName.toLowerCase(java.util.Locale.ROOT).endsWith(".json")) {
            throw TemplateException.invalid("INVALID_LAYOUT_FILE", "Chỉ hỗ trợ file bố cục có đuôi .json");
        }
        if (layoutJson.getSize() == 0 || layoutJson.getSize() > 10 * 1024 * 1024) {
            throw TemplateException.invalid("LAYOUT_SIZE_LIMIT", "File JSON bố cục phải lớn hơn 0 và không quá 10 MB");
        }
        JsonNode layout;
        try (var input = layoutJson.getInputStream()) {
            layout = mapper.readTree(input);
        } catch (Exception ex) {
            throw TemplateException.invalid("INVALID_LAYOUT_JSON", "File bố cục không phải JSON hợp lệ");
        }
        if (layout == null || !layout.isObject()) {
            throw TemplateException.invalid("INVALID_LAYOUT_JSON", "Bố cục phải là một JSON object");
        }
        return layout;
    }

    private void applyLayoutJson(FormInvoiceEntity form, JsonNode layout) {
        var user = currentUser();
        var access = new TemplateAccess(user.getCompanyId(), user.getId(), true);
        var details = formTemplates.editable(form.getId(), "admin-layout-upload-" + UUID.randomUUID(), access, null);
        templates.save(details.template().getId(), details.draft().getLockVersion(), layout, access);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> setStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        permission("form-invoice-manage");
        Integer status;
        try {
            status = body.get("status") == null ? null : Integer.valueOf(body.get("status").toString());
        } catch (NumberFormatException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "Trạng thái không hợp lệ"));
        }
        if (status == null || !Set.of(0, 1).contains(status)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Trạng thái không hợp lệ"));
        }

        try {
            return transactionTemplate.execute(txStatus -> {
                FormInvoiceEntity current = service.findById(id).orElse(null);
                if (current == null || !Integer.valueOf(1).equals(current.getSystem())) {
                    return ResponseEntity.notFound().build();
                }
                if (status == 1) {
                    var user = currentUser();
                    if (user == null) return ResponseEntity.status(403).build();
                    formTemplates.saveInitialVersion(id, new TemplateAccess(user.getCompanyId(), user.getId(), true));
                }
                FormInvoiceEntity patch = new FormInvoiceEntity();
                patch.setStatus(status);
                FormInvoiceEntity updated = service.update(id, patch).orElse(null);
                return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
            });
        } catch (TemplateException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("setStatus error", ex);
            return ResponseEntity.status(500).body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Không thể cập nhật trạng thái"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        permission("form-invoice-manage");
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        if (!Integer.valueOf(1).equals(e.getSystem())) return ResponseEntity.notFound().build();


        try { service.delete(id); }
        catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage())); }
        return ResponseEntity.noContent().build();
    }
}
