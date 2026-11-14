package vn.hoadon.controllers.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.invoice.template.service.InvoiceDocumentBridge;
import vn.hoadon.invoice.template.service.TemplateAccess;
import vn.hoadon.services.FormInvoiceService;
import vn.hoadon.util.UploadPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/administrator/form-invoices")
public class FormInvoiceAdminController extends BaseController {

    private final FormInvoiceService service;
    @Autowired private InvoiceDocumentBridge documents;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FormInvoiceAdminController.class);

    @Autowired
    public FormInvoiceAdminController(FormInvoiceService service) {
        this.service = service;
    }

   @PostMapping("/list")
public ResponseEntity<?> list(@RequestBody Map<String, Object> body,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size) {
    permission("form-invoice-manage");

    var result = service.pageByCompanySystemWithFilters(null, 0, java.util.Objects.toString(body.get("q"), ""), getInt(body, "category"), getInt(body, "type"), getInt(body, "status"), PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.DESC, "updatedAt")));
    return ResponseEntity.ok(Map.of("items", result.getContent(), "total", result.getTotalElements(), "per_page", result.getSize(), "current_page", result.getNumber() + 1, "last_page", Math.max(1, result.getTotalPages())));
}

// Hàm hỗ trợ lấy số an toàn từ dữ liệu ánh xạ
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
        if (!Integer.valueOf(0).equals(e.getSystem())) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(e);
    }

    @GetMapping(value = "/{id}/preview", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> preview(@PathVariable Long id) {
        permission("form-invoice-manage");
        var user = currentUser();
        var access = new TemplateAccess(user.getCompanyId(), user.getId(), true);
        return documents.pdf(documents.sample(id, true, access), false, "invoice-" + id + ".pdf");
    }

    @PostMapping(value = "/saveOrUpdate")
    public ResponseEntity<?> saveOrUpdate(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "category", required = false) Integer category,
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "have_code", required = false) Integer haveCode,
            @RequestParam(value = "photo", required = false) MultipartFile photo
    ) {
        permission("form-invoice-manage");
        log.debug("saveOrUpdate called: id={}, name={}, category={}, type={}, status={}, haveCode={}", id, name, category, type, status, haveCode);

        try {
            FormInvoiceEntity e;
            boolean creating = (id == null);
            if (creating) {
                e = new FormInvoiceEntity();
                // Save creating user's id
                try { e.setUserId(currentUser() != null ? currentUser().getId() : null); } catch (Exception ignore) { e.setUserId(null); }
                e.setCreatedAt(LocalDateTime.now());
            } else {
                Optional<FormInvoiceEntity> opt = service.findById(id);
                if (opt.isEmpty()) return ResponseEntity.notFound().build();
                if (!Integer.valueOf(0).equals(opt.get().getSystem())) return ResponseEntity.notFound().build();
                e = new FormInvoiceEntity();
                e.setId(id);
                e.setCompanyId(opt.get().getCompanyId());
            }

            if (!StringUtils.hasText(name)) return ResponseEntity.badRequest().body(Map.of("message", "Name required"));
            e.setName(name);
            e.setCategory(category != null ? category : 1); // mặc định VAT
            e.setType(type != null ? type : 1);           // mặc định một thuế suất
            e.setStatus(status != null ? status : 0);
            e.setHaveCode(haveCode != null ? haveCode : 1); // mặc định: Có mã

            // Tự detect company_id từ user hiện tại, fallback = 1
            Long detectedCompanyId = 1L;
            try {
                var cu = currentUser();
                if (cu != null) {
                    try {
                        java.lang.reflect.Method m = cu.getClass().getMethod("getCompanyId");
                        Object v = m.invoke(cu);
                        if (v instanceof Number) {
                            detectedCompanyId = ((Number) v).longValue();
                        }
                    } catch (Exception ignore) {
                        // keep default 1L
                    }
                }
            } catch (Exception ignore) {
                // keep default 1L
            }
            
            if (!creating && e.getCompanyId() != null) detectedCompanyId = e.getCompanyId();
            e.setCompanyId(detectedCompanyId);
            e.setSystem(0); // luôn là mẫu hệ thống
            
            // LUÔN set mặc định form_code & serial (BẮT BUỘC để tránh NULL)
            e.setFormCode("X");
            e.setSerial("XXXXXX");

            // Resolve upload company id theo company_id đã detect
            Long uploadCompanyId = detectedCompanyId;

            if (photo != null && !photo.isEmpty()) {
                try {
                    Path dir = UploadPath.resolveCompanyTypeDir(uploadCompanyId, "photo");
                    String orig = photo.getOriginalFilename();
                    String ext = (orig != null && orig.contains(".")) ? orig.substring(orig.lastIndexOf('.')) : "";
                    String fname = UUID.randomUUID().toString() + ext;
                    Path target = dir.resolve(fname);
                    Files.write(target, photo.getBytes());
                    e.setPhoto(UploadPath.publicUrl(uploadCompanyId, "photo", fname));
                } catch (IOException ex) {
                    return ResponseEntity.status(500).body(Map.of("message", "Failed saving photo: " + ex.getMessage()));
                }
            }

            e.setUpdatedAt(LocalDateTime.now());

            FormInvoiceEntity saved;
            if (creating) {
                saved = service.create(e);
            } else {
                // Dùng update để tránh kiểm tra trùng với chính bản ghi hiện tại
                saved = service.update(id, e).orElseThrow(() -> new RuntimeException("Unable to update template"));
            }
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException iae) {
            log.warn("Validation error: {}", iae.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception ex) {
            log.error("Error saving form invoice", ex);
            return ResponseEntity.status(500).body(Map.of("message", ex.getClass().getSimpleName() + ": " + (ex.getMessage() != null ? ex.getMessage() : "Unexpected error")));
        }
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> setStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        permission("form-invoice-manage");
        var current = service.findById(id).orElse(null);
        if (current == null || !Integer.valueOf(0).equals(current.getSystem())) return ResponseEntity.notFound().build();
        Integer status = body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : null;
        if (status == null) return ResponseEntity.badRequest().body(Map.of("message", "status required"));
        try {
            FormInvoiceEntity patch = new FormInvoiceEntity();
            patch.setStatus(status);
            FormInvoiceEntity updated = service.update(id, patch).orElse(null);
            if (updated == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            log.error("setStatus error", e);
            return ResponseEntity.status(500).body(Map.of("message", e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : "Error")));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        permission("form-invoice-manage");
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        if (!Integer.valueOf(0).equals(e.getSystem())) return ResponseEntity.notFound().build();


        try { service.delete(id); }
        catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage())); }
        return ResponseEntity.noContent().build();
    }
}
