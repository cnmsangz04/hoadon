package vn.hoadon.controllers.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.vatrate.VatRatesDto;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.entity.VatRatesEntity;
import vn.hoadon.services.VatRatesService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("v1/administrator/vat-rate")
public class VatRatesController extends BaseController {

    @Autowired
    private VatRatesService vatRatesService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((UserEntity) auth.getPrincipal()).getId();
    }

    // Paginated list for administrators
    @PostMapping("/list")
    public Map<String, Object> list(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        permission("vat-rate-list");
        Integer userId = Math.toIntExact(getCurrentUserId());
        Integer status = null;
        String keyword = null;
        if (body != null) {
            Object s = body.get("status");
            if (s != null && !s.toString().isBlank()) {
                try { status = Integer.valueOf(s.toString()); } catch (Exception ignored) {}
            }
            Object kw = body.get("keyword");
            if (kw != null) keyword = kw.toString();
        }
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.ASC, "prioritize"));
        Page<VatRatesEntity> p = vatRatesService.pageByUser(userId, status, pageable, keyword);

        List<Map<String, Object>> items = p.getContent().stream().map(it -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", it.getId());
            m.put("code", it.getCode());
            m.put("label", it.getLabel());
            m.put("status", it.getStatus());
            m.put("prioritize", it.getPrioritize());
            m.put("updatedAt", it.getUpdatedAt());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> res = new HashMap<>();
        res.put("items", items);
        res.put("total", p.getTotalElements());
        res.put("per_page", p.getSize());
        res.put("current_page", p.getNumber() + 1);
        res.put("last_page", Math.max(1, p.getTotalPages()));
        return res;
    }

    // 1️⃣ Create (nhận DTO)
    @PostMapping
    public VatRatesEntity create(@RequestBody VatRatesDto dto) {
        permission("vat-rate-save");
        VatRatesEntity entity = new VatRatesEntity();
        entity.setCode(dto.getCode());
        entity.setLabel(dto.getLabel());
        entity.setStatus(dto.getStatus());
        entity.setPrioritize(dto.getPrioritize() != null ? dto.getPrioritize() : 0);
        entity.setUserId(Math.toIntExact(getCurrentUserId()));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        return vatRatesService.create(entity);
    }

    // 2️⃣ Read - get all
    @GetMapping
    public List<VatRatesEntity> findAll() {
        permission("vat-rate-list");
        return vatRatesService.findAllOrderedByPrioritize();
    }

    // 3️⃣ Update (nhận DTO)
    @PutMapping("/{id}")
    public VatRatesEntity update(
            @PathVariable Integer id,
            @RequestBody VatRatesDto dto
    ) {
        permission("vat-rate-save");
        VatRatesEntity entity = new VatRatesEntity();
        entity.setLabel(dto.getLabel());
        entity.setCode(dto.getCode());
        entity.setStatus(dto.getStatus());
        if (dto.getPrioritize() != null) {
            entity.setPrioritize(dto.getPrioritize());
        }
        entity.setUpdatedAt(LocalDateTime.now());

        return vatRatesService.update(id, entity);
    }

    // 4️⃣ Delete
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        permission("vat-rate-delete");
        vatRatesService.delete(id);
    }

    @PutMapping("/{id}/toggle-status")
    public VatRatesEntity toggleStatus(@PathVariable Integer id) {
        permission("vat-rate-save");
        VatRatesEntity existing = vatRatesService.findById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TaxRate not found");
        }

        // Toggle Integer status: 1 <-> 0
        Integer currentStatus = existing.getStatus();
        existing.setStatus(currentStatus != null && currentStatus == 1 ? 0 : 1);
        existing.setUpdatedAt(LocalDateTime.now());

        return vatRatesService.update(id, existing);
    }

    // Reorder VAT rates by priority
    @PostMapping("/reorder")
    public void reorder(@RequestBody List<Integer> orderedIds) {
        permission("vat-rate-save");
        vatRatesService.reorder(orderedIds);
    }

}