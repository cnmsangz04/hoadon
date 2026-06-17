package vn.hoadon.controllers.admin;

import vn.hoadon.controllers.base.BaseController;
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
public class PermissionsController extends BaseController {

    @Autowired
    private PermissionService service;

    @PostMapping("/list")
    public ResponseEntity<Page<PermissionEntity>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Byte status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        permission("permission-manage");
        Pageable pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.asc("name")
        ));
        Page<PermissionEntity> data = service.list(keyword, categoryId, level, status, pageable);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermissionEntity> get(@PathVariable Long id) {
        permission("permission-manage");
        Optional<PermissionEntity> entity = service.findById(id);
        return ResponseEntity.of(entity);
    }

    @PostMapping("/saveOrUpdate")
    public ResponseEntity<PermissionEntity> saveOrUpdate(@RequestBody PermissionCreateDTO dto) {
        permission("permission-manage");
        PermissionEntity saved = service.saveOrUpdate(dto);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permission("permission-manage");
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam byte status) {
        permission("permission-manage");
        service.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }
}
