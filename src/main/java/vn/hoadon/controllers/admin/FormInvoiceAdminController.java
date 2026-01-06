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

    // 1. Lấy dữ liệu an toàn từ Body
    Integer companyId = getInt(body, "companyId");
    Long userId = getLong(body, "userId");

    if (userId == null) {
        try {
            var cu = currentUser();
            if (cu != null) userId = cu.getId();
        } catch (Exception ignore) {}
    }

    // 2. Gom dữ liệu (Sử dụng Set để tự động deduplicate ngay từ đầu nếu cần)
    java.util.List<FormInvoiceEntity> combined = new java.util.ArrayList<>();
    Pageable limit1000 = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "updatedAt"));

    if (companyId != null) {
        combined.addAll(service.pageByCompanySystem(companyId.longValue(), 1, limit1000).getContent());
    } else {
        combined.addAll(service.pageBySystem(0, limit1000).getContent());
    }
    
    if (userId != null) {
        combined.addAll(service.pageByUser(userId, limit1000).getContent());
    }

    // 3. Deduplicate và chuyển thành danh sách có thể chỉnh sửa (ArrayList)
    java.util.Map<Long, FormInvoiceEntity> map = new java.util.LinkedHashMap<>();
    for (FormInvoiceEntity it : combined) {
        if (it != null && it.getId() != null) map.put(it.getId(), it);
    }
    
    // Quan trọng: Sử dụng ArrayList để có thể Sort và Filter
    java.util.List<FormInvoiceEntity> filtered = new java.util.ArrayList<>(map.values());

    // 4. Lọc dữ liệu (Sử dụng stream và thu thập vào list mới)
    String q = body.getOrDefault("q", "").toString().toLowerCase();
    Integer category = getInt(body, "category");
    Integer type = getInt(body, "type");
    Integer status = getInt(body, "status");
    Integer system = getInt(body, "system");

    filtered = filtered.stream().filter(it -> {
        if (StringUtils.hasText(q) && (it.getName() == null || !it.getName().toLowerCase().contains(q))) return false;
        if (category != null && !java.util.Objects.equals(it.getCategory(), category)) return false;
        if (type != null && !java.util.Objects.equals(it.getType(), type)) return false;
        if (status != null && !java.util.Objects.equals(it.getStatus(), status)) return false;
        if (system != null && !java.util.Objects.equals(it.getSystem(), system)) return false;
        return true;
    }).collect(java.util.stream.Collectors.toList()); // Dùng collect thay vì .toList() để an toàn

    // 5. Sắp xếp (Lúc này filtered là ArrayList nên sort thoải mái)
    filtered.sort((a, b) -> {
        java.time.LocalDateTime aa = a.getUpdatedAt();
        java.time.LocalDateTime bb = b.getUpdatedAt();
        if (aa == null && bb == null) return 0;
        if (aa == null) return 1;
        if (bb == null) return -1;
        return bb.compareTo(aa);
    });

    // 6. Phân trang in-memory
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

// Hàm hỗ trợ lấy số an toàn từ Map
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
            @RequestParam(value = "serial", required = false) String serialFull,
            @RequestParam(value = "have_code", required = false) Integer haveCode,
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "photo", required = false) MultipartFile photo
    ) {
        permission("form-invoice-manage");
        log.debug("saveOrUpdate called: id={}, name={}, category={}, type={}, status={}, haveCode={}, companyId={}, serial={}", id, name, category, type, status, haveCode, companyId, serialFull);

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

            // Apply company/system assignment: if companyId provided, this is a company template (system=1)
            e.setCompanyId(companyId);
            e.setSystem(companyId != null ? 1 : 0);

            if (!StringUtils.hasText(name)) return ResponseEntity.badRequest().body(Map.of("message", "Name required"));
            e.setName(name);
            e.setCategory(category);
            e.setType(type);
            e.setStatus(status != null ? status : 0);
            e.setHaveCode(haveCode != null ? haveCode : 0);

            // Parse serial into formCode and serial remainder if provided
            if (serialFull != null && !serialFull.isBlank()) {
                String s = serialFull.trim();
                if (s.length() >= 2) {
                    e.setFormCode(s.substring(0, 1));
                    e.setSerial(s.substring(1));
                } else {
                    return ResponseEntity.badRequest().body(Map.of("message", "Invalid serial"));
                }
            }

            // Resolve upload company id (use 0 for global/system templates)
            Long uploadCompanyId = (e.getCompanyId() != null) ? e.getCompanyId() : 0L;

            // Handle uploaded files: store under uploads/<companyId>/template and uploads/<companyId>/photo
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
                // Use update to avoid duplicate-checking against itself
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
