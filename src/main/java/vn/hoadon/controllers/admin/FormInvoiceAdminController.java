package vn.hoadon.controllers.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.services.FormInvoiceService;
import vn.hoadon.util.UploadPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/administrator/form-invoices")
public class FormInvoiceAdminController extends BaseController {

    private final FormInvoiceService service;
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

    // CHỈ HIỂN THỊ system = 0 (mẫu hệ thống)
    Pageable limit1000 = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "updatedAt"));
    java.util.List<FormInvoiceEntity> combined = new java.util.ArrayList<>(
        service.pageBySystem(0, limit1000).getContent()
    );

    // Deduplicate
    java.util.Map<Long, FormInvoiceEntity> map = new java.util.LinkedHashMap<>();
    for (FormInvoiceEntity it : combined) {
        if (it != null && it.getId() != null) map.put(it.getId(), it);
    }
    
    java.util.List<FormInvoiceEntity> filtered = new java.util.ArrayList<>(map.values());

    // Lọc dữ liệu theo các tham số
    String q = body.getOrDefault("q", "").toString().toLowerCase();
    Integer category = getInt(body, "category");
    Integer type = getInt(body, "type");
    Integer status = getInt(body, "status");

    filtered = filtered.stream().filter(it -> {
        // Luôn lọc chỉ system = 0
        if (!java.util.Objects.equals(it.getSystem(), 0)) return false;
        if (StringUtils.hasText(q) && (it.getName() == null || !it.getName().toLowerCase().contains(q))) return false;
        if (category != null && !java.util.Objects.equals(it.getCategory(), category)) return false;
        if (type != null && !java.util.Objects.equals(it.getType(), type)) return false;
        if (status != null && !java.util.Objects.equals(it.getStatus(), status)) return false;
        return true;
    }).collect(java.util.stream.Collectors.toList());

    // Sắp xếp
    filtered.sort((a, b) -> {
        java.time.LocalDateTime aa = a.getUpdatedAt();
        java.time.LocalDateTime bb = b.getUpdatedAt();
        if (aa == null && bb == null) return 0;
        if (aa == null) return 1;
        if (bb == null) return -1;
        return bb.compareTo(aa);
    });

    // Phân trang in-memory
    int total = filtered.size();
    int from = Math.max(0, Math.min(total, page * size));
    int to = Math.min(total, from + size);
    java.util.List<FormInvoiceEntity> pageItems = filtered.subList(from, to);

    Map<String, Object> res = new HashMap<>();
    res.put("items", pageItems);
    res.put("total", total);
    res.put("per_page", size);
    res.put("current_page", page + 1);
    res.put("last_page", Math.max(1, (total + size - 1) / size));
    
    return ResponseEntity.ok(res);
}

// Hàm hỗ trợ lấy số an toàn từ dữ liệu ánh xạ
private Integer getInt(Map<String, Object> body, String key) {
    Object val = body.get(key);
    if (val == null) return null;
    if (val instanceof Number) return ((Number) val).intValue();
    try { return Integer.valueOf(val.toString()); } catch (Exception e) { return null; }
}

private Long getLong(Map<String, Object> body, String key) {
    Object val = body.get(key);
    if (val == null) return null;
    if (val instanceof Number) return ((Number) val).longValue();
    try { return Long.valueOf(val.toString()); } catch (Exception e) { return null; }
}
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        permission("form-invoice-manage");
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        return ResponseEntity.ok(e);
    }

    @PostMapping(value = "/saveOrUpdate")
    public ResponseEntity<?> saveOrUpdate(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "category", required = false) Integer category,
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "have_code", required = false) Integer haveCode,
            @RequestParam(value = "file", required = false) MultipartFile file,
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
                e = opt.get();
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
            
            e.setCompanyId(detectedCompanyId);
            e.setSystem(0); // luôn là mẫu hệ thống
            
            // LUÔN set mặc định form_code & serial (BẮT BUỘC để tránh NULL)
            e.setFormCode("X");
            e.setSerial("XXXXXX");

            // Resolve upload company id theo company_id đã detect
            Long uploadCompanyId = detectedCompanyId;

            // Xử lý file tải lên: lưu dưới uploads/<companyId>/template và uploads/<companyId>/photo
            if (file != null && !file.isEmpty()) {
                try {
                    Path dir = UploadPath.resolveCompanyTypeDir(uploadCompanyId, "template");
                    String orig = file.getOriginalFilename();
                    String ext = (orig != null && orig.contains(".")) ? orig.substring(orig.lastIndexOf('.')) : "";
                    String fname = UUID.randomUUID().toString() + ext;
                    Path target = dir.resolve(fname);
                    Files.write(target, file.getBytes());
                    e.setFile(UploadPath.publicUrl(uploadCompanyId, "template", fname));
                } catch (IOException ex) {
                    return ResponseEntity.status(500).body(Map.of("message", "Failed saving file: " + ex.getMessage()));
                }
            }

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


        // Remove files
        try {
            if (StringUtils.hasText(e.getPhoto())) {
                String rel = e.getPhoto().replaceFirst("^/", "");
                Path p = Paths.get(System.getProperty("user.dir")).resolve(rel);
                Files.deleteIfExists(p);
            }
        } catch (Exception ignore) {}
        try {
            if (StringUtils.hasText(e.getFile())) {
                String rel = e.getFile().replaceFirst("^/", "");
                Path p = Paths.get(System.getProperty("user.dir")).resolve(rel);
                Files.deleteIfExists(p);
            }
        } catch (Exception ignore) {}

        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
