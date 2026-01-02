package vn.hoadon.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.forminvoice.FormInvoiceListItemDto;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.services.FormInvoiceService;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/v1/form-invoices")
public class FormInvoiceController extends BaseController {

    private final FormInvoiceService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    public FormInvoiceController(FormInvoiceService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) Integer type
    ) {
        UserEntity user = currentUser();
        Long companyId = user != null ? user.getCompanyId() : null;
        if (companyId == null) {
            return empty(page, size);
        }
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<FormInvoiceEntity> p = service.pageByCompanySystem(companyId, 1, pageable);

        List<FormInvoiceEntity> filtered = p.getContent();
        if (q != null && !q.isBlank()) {
            String kw = q.toLowerCase();
            filtered = filtered.stream().filter(it -> {
                String name = it.getName() != null ? it.getName().toLowerCase() : "";
                String serial = it.getSerial() != null ? it.getSerial().toLowerCase() : "";
                return name.contains(kw) || serial.contains(kw);
            }).toList();
        }
        if (category != null) {
            filtered = filtered.stream().filter(it -> Objects.equals(category, it.getCategory())).toList();
        }
        if (type != null) {
            filtered = filtered.stream().filter(it -> Objects.equals(type, it.getType())).toList();
        }

        // Resolve usernames for displayed items
        Set<Long> uidSet = new HashSet<>();
        for (FormInvoiceEntity it : filtered) {
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

        List<FormInvoiceListItemDto> items = mapToDto(filtered, usernameMap);
        Map<String, Object> res = new HashMap<>();
        res.put("items", items);
        res.put("total", p.getTotalElements());
        res.put("per_page", p.getSize());
        res.put("current_page", p.getNumber() + 1);
        res.put("last_page", Math.max(1, p.getTotalPages()));
        return res;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(403).body(Map.of("message", "Không xác định được công ty"));
        }
        String name = Optional.ofNullable(body.get("name")).map(Object::toString).orElse(null);
        String serial = Optional.ofNullable(body.get("serial")).map(Object::toString).orElse(null);
        Integer category = Optional.ofNullable(body.get("category")).map(v -> Integer.valueOf(v.toString())).orElse(null);
        Integer type = Optional.ofNullable(body.get("type")).map(v -> Integer.valueOf(v.toString())).orElse(null);
        Integer status = Optional.ofNullable(body.get("status")).map(v -> Integer.valueOf(v.toString())).orElse(1);

        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên mẫu không được để trống"));
        }
        FormInvoiceEntity e = new FormInvoiceEntity();
        e.setCompanyId(user.getCompanyId());
        e.setUserId(user.getId());
        e.setName(name);
        e.setSerial(serial);
        e.setCategory(category);
        e.setType(type);
        e.setStatus(status);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());

        FormInvoiceEntity saved = service.create(e);
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", saved.getId());
        resp.put("message", "Đã tạo mẫu hóa đơn");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/users/by-ids")
    public List<Map<String, Object>> usersByIds(@RequestParam String ids) {
        if (ids == null || ids.isBlank()) return List.of();
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> s.matches("^\\d+$"))
                .map(Long::parseLong)
                .toList();
        if (idList.isEmpty()) return List.of();
        List<UserEntity> users = userRepository.findAllById(idList);
        return users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("name", u.getName());
            return m;
        }).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        if (!Objects.equals(e.getCompanyId(), user.getCompanyId())) {
            return ResponseEntity.status(403).build();
        }
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private List<FormInvoiceListItemDto> mapToDto(List<FormInvoiceEntity> list, Map<Long, String> usernameMap) {
        return list.stream().map(it -> {
            FormInvoiceListItemDto d = new FormInvoiceListItemDto();
            d.setId(it.getId());
            d.setName(it.getName());
            d.setSerial(it.getSerial());
            d.setCategory(it.getCategory());
            d.setCategoryLabel(categoryLabel(it.getCategory()));
            d.setType(it.getType());
            d.setTypeLabel(typeLabel(it.getType()));
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
            default -> "";
        };
    }
    private String typeLabel(Integer v) {
        if (v == null) return "";
        return switch (v) {
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
}