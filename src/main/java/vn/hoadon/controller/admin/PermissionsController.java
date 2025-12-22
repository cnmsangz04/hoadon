package vn.hoadon.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.dto.permission.PermissionCreateDTO;
import vn.hoadon.entity.PermissionEntity;
import vn.hoadon.services.PermissionService;

import java.util.Optional;

@RestController
@RequestMapping("/v1/administrator/permissions")
public class PermissionsController {

    @Autowired
    private PermissionService service;

    @PostMapping("/list")
    public ResponseEntity<Page<PermissionEntity>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PermissionEntity> data = service.list(keyword, pageable);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermissionEntity> get(@PathVariable Long id) {
        Optional<PermissionEntity> entity = service.findById(id);
        return ResponseEntity.of(entity);
    }

    @PostMapping("/saveOrUpdate")
    public ResponseEntity<PermissionEntity> saveOrUpdate(@RequestBody PermissionCreateDTO dto) {
        PermissionEntity saved = service.saveOrUpdate(dto);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
