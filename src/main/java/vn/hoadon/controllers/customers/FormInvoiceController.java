package vn.hoadon.controllers.customers;

import vn.hoadon.invoice.template.service.InvoiceDocumentBridge;
import vn.hoadon.invoice.template.service.InvoiceTemplateBindingService;
import vn.hoadon.invoice.template.service.TemplateAccess;
import vn.hoadon.invoice.template.service.TemplateException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.forminvoice.FormInvoiceListItemDto;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.repositories.RegisterInvoiceRepository;
import vn.hoadon.services.FormInvoiceService;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@RestController
@RequestMapping("/v1/form-invoices")
public class FormInvoiceController extends BaseController {

    private final FormInvoiceService service;

    @Autowired private UserRepository userRepository;
    @Autowired private RegisterInvoiceRepository registerInvoiceRepository;

    @Autowired
    public FormInvoiceController(FormInvoiceService service) {
        this.service = service;
    }

    @Autowired private InvoiceDocumentBridge documentBridge;
    @Autowired private InvoiceTemplateBindingService templateBindings;
    @Autowired private vn.hoadon.invoice.template.service.InvoiceFormTemplateService formTemplates;

    @GetMapping("/capabilities")
    public Map<String, Object> capabilities() {
        permission("form-invoice-list|form-invoice-manage");
        return Map.of("versionedTemplates", true, "customTemplates", true, "pdfImport", false);
    }

    @PostMapping("/{id}/template-version")
    public ResponseEntity<Void> bindVersion(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        permission("form-invoice-save");
        var user = currentUser();
        templateBindings.bindForm(id, body.get("versionId"), new TemplateAccess(user.getCompanyId(), user.getId(), false));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status
    ) {
        permission("form-invoice-list");
        UserEntity user = currentUser();
        Long companyId = user != null ? user.getCompanyId() : null;
        if (companyId == null) {
            return empty(page, size);
        }
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<FormInvoiceEntity> p = service.pageByCompanySystemWithFilters(companyId, 0, q, category, type, status, pageable);

        List<FormInvoiceEntity> items = p.getContent();

        Set<Long> uidSet = new HashSet<>();
        for (FormInvoiceEntity it : items) {
            if (it.getUserId() != null) uidSet.add(it.getUserId());
        }
        Map<Long, String> usernameMap = new HashMap<>();
        if (!uidSet.isEmpty()) {
            List<UserEntity> users = userRepository.findAllById(uidSet);
            for (UserEntity u : users) {
                if (u.getId() != null) {
                    usernameMap.put(u.getId(), Optional.ofNullable(u.getUsername()).orElse(u.getName()));
                }
            }
        }

        List<FormInvoiceListItemDto> dtoItems = mapToDto(items, usernameMap);
        Map<String, Object> res = new HashMap<>();
        res.put("items", dtoItems);
        res.put("total", p.getTotalElements());
        res.put("per_page", p.getSize());
        res.put("current_page", p.getNumber() + 1);
        res.put("last_page", Math.max(1, p.getTotalPages()));
        return res;
    }

    @GetMapping("/templates")
    public Map<String, Object> listTemplates(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        permission("form-invoice-list|form-invoice-save");
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
        Page<FormInvoiceEntity> result = service.pageByCompanySystemWithFilters(null, 1, q, category, type, 1, pageable);
        List<Map<String, Object>> items = result.getContent().stream().map(form -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", form.getId());
            item.put("name", form.getName());
            item.put("photo", form.getPhoto());
            item.put("type", form.getType());
            item.put("category", form.getCategory());
            item.put("stockIssueType", form.getStockIssueType());
            return item;
        }).toList();
        return Map.of("items", items, "total", result.getTotalElements(), "per_page", result.getSize(),
                "current_page", result.getNumber() + 1, "last_page", Math.max(1, result.getTotalPages()));
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<?> getTemplate(@PathVariable Long id) {
        permission("form-invoice-list|form-invoice-save");
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        if (Objects.equals(e.getSystem(), 0)
                && (!Objects.equals(e.getCompanyId(), user.getCompanyId()) || !Set.of(0, 1).contains(e.getStatus()))) {
            return ResponseEntity.status(403).build();
        }
        if (Objects.equals(e.getSystem(), 1) && !Objects.equals(e.getStatus(), 1)) {
            return ResponseEntity.status(403).build();
        }
        if (!Objects.equals(e.getSystem(), 0) && !Objects.equals(e.getSystem(), 1)) {
            return ResponseEntity.status(403).build();
        }
        Map<String, Object> item = new HashMap<>();
        item.put("id", e.getId());
        item.put("name", e.getName());
        item.put("serial", e.getSerial());
        item.put("templateVersionId", e.getTemplateVersionId());
        item.put("photo", e.getPhoto());
        item.put("type", e.getType());
        item.put("category", e.getCategory());
        item.put("stockIssueType", e.getStockIssueType());
        return ResponseEntity.ok(item);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        permission("form-invoice-list|form-invoice-save");
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        if (!Objects.equals(e.getCompanyId(), user.getCompanyId()) || !Objects.equals(e.getSystem(), 0)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(e);
    }

    @GetMapping(value = "/{id}/view", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> viewHtml(@PathVariable Long id) {
        permission("form-invoice-list");
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity form = opt.get();
        if (!Objects.equals(form.getCompanyId(), user.getCompanyId()) || !Objects.equals(form.getSystem(), 0)) return ResponseEntity.status(403).build();
        var access = new TemplateAccess(user.getCompanyId(), user.getId(), false);
        return documentBridge.sampleHtml(id, false, access);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        permission("form-invoice-save");
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(403).body(Map.of("message", "Không xác định được công ty"));
        }
        Long companyId = user.getCompanyId();

        String name = Optional.ofNullable(body.get("name")).map(Object::toString).orElse(null);
        String serialFull = Optional.ofNullable(body.get("serial")).map(Object::toString).orElse(null);
        Integer category = Optional.ofNullable(body.get("category")).map(v -> Integer.valueOf(v.toString())).orElse(null);
        Integer type = Optional.ofNullable(body.get("type")).map(v -> Integer.valueOf(v.toString())).orElse(null);
        Integer status = Optional.ofNullable(body.get("status")).map(v -> Integer.valueOf(v.toString())).orElse(0);
        Long templateId = Optional.ofNullable(body.get("templateId")).map(v -> Long.valueOf(v.toString())).orElse(null);
        Integer haveCode = Optional.ofNullable(body.get("have_code")).map(v -> Integer.valueOf(v.toString())).orElse(0);
        Integer stockIssueType = Optional.ofNullable(body.get("stockIssueType")).map(v -> Integer.valueOf(v.toString())).orElse(null);

        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên mẫu không được để trống"));
        }
        if (serialFull == null || serialFull.trim().length() < 2) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ký hiệu không hợp lệ"));
        }
        serialFull = serialFull.trim();
        String formCode = serialFull.substring(0, 1);
        String serialRemainder = serialFull.substring(1);

        if (templateId != null) {
            Optional<FormInvoiceEntity> optTpl = service.findById(templateId);
            if (optTpl.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy mẫu template"));
            }
            FormInvoiceEntity tpl = optTpl.get();
            if (!Objects.equals(tpl.getSystem(), 1)) {
                return ResponseEntity.status(403).body(Map.of("message", "Chỉ được sao chép từ mẫu hệ thống"));
            }
            if (!Objects.equals(tpl.getCategory(), category) || !Objects.equals(tpl.getType(), type) || !Objects.equals(tpl.getStatus(), 1)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mẫu nguồn không phù hợp cấu hình hoặc đã ngừng sử dụng"));
            }
            if (stockIssueType == null) stockIssueType = tpl.getStockIssueType();
            if (Objects.equals(category, 6) && !Objects.equals(stockIssueType, tpl.getStockIssueType())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mẫu nguồn không phù hợp loại phiếu xuất kho"));
            }
        }

        FormInvoiceEntity e = new FormInvoiceEntity();
        if (templateId != null) {
            e.setTemplateVersionId(formTemplates.latestPublished(templateId).getId());
        }
        e.setCompanyId(companyId);
        e.setUserId(user.getId());
        e.setName(name);
        e.setFormCode(formCode);
        e.setSerial(serialRemainder);
        e.setCategory(category);
        e.setType(type);
        boolean activateAfterCreate = Integer.valueOf(1).equals(status);
        e.setStatus(0);
        e.setSystem(0);
        e.setHaveCode(haveCode);
        e.setStockIssueType(stockIssueType);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());

        FormInvoiceEntity saved;
        try {
            saved = service.create(e, null);
            if (activateAfterCreate) {
                var access = new TemplateAccess(companyId, user.getId(), false);
                formTemplates.saveInitialVersion(saved.getId(), access);
                FormInvoiceEntity activation = new FormInvoiceEntity();
                activation.setStatus(1);
                saved = service.update(saved.getId(), activation)
                        .orElseThrow(() -> new IllegalArgumentException("Không thể kích hoạt mẫu hóa đơn"));
            }
        } catch (IllegalArgumentException iae) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (TemplateException te) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.badRequest().body(Map.of("message", te.getMessage(), "code", te.getCode()));
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", saved.getId());
        resp.put("message", "Đã tạo mẫu hóa đơn");
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        permission("form-invoice-save");
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity ex = opt.get();
        if (!Objects.equals(ex.getCompanyId(), user.getCompanyId()) || !Objects.equals(ex.getSystem(), 0)) return ResponseEntity.status(403).build();

        FormInvoiceEntity patch = new FormInvoiceEntity();
        patch.setName(Optional.ofNullable(body.get("name")).map(Object::toString).orElse(null));
        String serialFull = Optional.ofNullable(body.get("serial")).map(Object::toString).orElse(null);
        if (serialFull != null) {
            String s = serialFull.trim();
            if (s.length() >= 2) {
                patch.setFormCode(s.substring(0,1));
                patch.setSerial(s.substring(1));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Ký hiệu không hợp lệ"));
            }
        }
        patch.setCategory(Optional.ofNullable(body.get("category")).map(v -> Integer.valueOf(v.toString())).orElse(null));
        patch.setType(Optional.ofNullable(body.get("type")).map(v -> Integer.valueOf(v.toString())).orElse(null));
        patch.setStatus(Optional.ofNullable(body.get("status")).map(v -> Integer.valueOf(v.toString())).orElse(null));
        patch.setSystem(0);

        patch.setHaveCode(Optional.ofNullable(body.get("have_code")).map(v -> Integer.valueOf(v.toString())).orElse(null));
        patch.setStockIssueType(Optional.ofNullable(body.get("stockIssueType")).map(v -> Integer.valueOf(v.toString())).orElse(null));

        if (Objects.equals(patch.getStatus(), 1) && !hasAcceptedRegisterInvoice(user.getCompanyId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chưa có tờ khai hóa đơn được chấp nhận nên không thể kích hoạt mẫu hóa đơn"));
        }

        Optional<FormInvoiceEntity> updated;
        try {
            updated = service.update(id, patch);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        }
        if (updated.isEmpty()) return ResponseEntity.status(500).body(Map.of("message", "Cập nhật thất bại"));

        return ResponseEntity.ok(Map.of("message", "Đã cập nhật mẫu"));
    }

    private boolean hasAcceptedRegisterInvoice(Long companyId) {
        if (companyId == null) return false;
        List<?> accepted = registerInvoiceRepository.findLatestAccepted(companyId, PageRequest.of(0, 1));
        return accepted != null && !accepted.isEmpty();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permission("form-invoice-save");
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        if (!Objects.equals(e.getCompanyId(), user.getCompanyId()) || !Objects.equals(e.getSystem(), 0)) {
            return ResponseEntity.status(403).build();
        }
        try { service.delete(id); }
        catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().build(); }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/by-ids")
    public List<Map<String, Object>> usersByIds(@RequestParam String ids) {
        permission("form-invoice-list");
        UserEntity current = currentUser();
        if (current == null || current.getCompanyId() == null) return List.of();
        if (ids == null || ids.isBlank()) return List.of();
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> s.matches("^\\d+$"))
                .map(Long::parseLong)
                .toList();
        if (idList.isEmpty()) return List.of();
        List<UserEntity> users = userRepository.findAllById(idList);
        return users.stream().filter(u -> u != null && Objects.equals(u.getCompanyId(), current.getCompanyId())).map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("name", u.getName());
            return m;
        }).toList();
    }

    private List<FormInvoiceListItemDto> mapToDto(List<FormInvoiceEntity> list, Map<Long, String> usernameMap) {
        return list.stream().map(it -> {
            FormInvoiceListItemDto d = new FormInvoiceListItemDto();
            d.setId(it.getId());
            d.setName(it.getName());
            String combinedSerial = (it.getFormCode() != null ? it.getFormCode() : "") + (it.getSerial() != null ? it.getSerial() : "");
            d.setSerial(combinedSerial);
            d.setCategory(it.getCategory());
            d.setStockIssueType(it.getStockIssueType());
            d.setCategoryLabel(categoryLabel(it.getCategory()));
            d.setType(it.getType());
            d.setTypeLabel(typeLabel(it.getType()));
            d.setStatus(it.getStatus());
            d.setUserId(it.getUserId());
            d.setUsername(it.getUserId() != null ? usernameMap.get(it.getUserId()) : null);
            d.setUpdatedAt(it.getUpdatedAt());
            return d;
        }).toList();
    }

    private String categoryLabel(Integer v) {
        if (v == null) return "";
        return switch (v) {
            case 1 -> "Hóa đơn giá trị gia tăng";
            case 2 -> "Hóa đơn bán hàng";
            case 6 -> "Phiếu xuất kho";
            default -> "";
        };
    }
    private String typeLabel(Integer v) {
        if (v == null) return "";
        return switch (v) {
            case -1 -> "Đã bao gồm thuế";
            case 0 -> "Không sử dụng thuế suất";
            case 1 -> "Một thuế suất";
            case 2 -> "Nhiều thuế suất";
            default -> "";
        };
    }

    private Map<String, Object> empty(int page, int size) {
        Map<String, Object> res = new HashMap<>();
        res.put("items", List.of());
        res.put("total", 0);
        res.put("per_page", size);
        res.put("current_page", page);
        res.put("last_page", 1);
        return res;
    }

    @GetMapping(value = "/{id}/download-xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> xmlDownload(@PathVariable Long id) {
        permission("form-invoice-list");
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        if (!Objects.equals(e.getCompanyId(), user.getCompanyId()) || !Objects.equals(e.getSystem(), 0)) return ResponseEntity.status(403).build();

        var access = new TemplateAccess(user.getCompanyId(), user.getId(), false);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("invoice-" + id + ".xml").build().toString())
                .body(documentBridge.sampleXml(id, false, access));
    }

    @GetMapping(value = "/{id}/download-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> pdfDownload(@PathVariable Long id, @RequestParam(required = false) Long artifactId) {
        permission("form-invoice-list");
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        if (!Objects.equals(e.getCompanyId(), user.getCompanyId()) || !Objects.equals(e.getSystem(), 0)) return ResponseEntity.status(403).build();

        var access = new TemplateAccess(user.getCompanyId(), user.getId(), false);
        return documentBridge.pdf(documentBridge.sampleArtifact(id, false, access, artifactId), true, "invoice-" + id + ".pdf");
    }
}
