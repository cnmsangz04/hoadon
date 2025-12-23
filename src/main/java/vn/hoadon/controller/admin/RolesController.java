package vn.hoadon.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.dto.role.RoleCloneDTO;
import vn.hoadon.entity.RoleEntity;
import vn.hoadon.services.RoleService;

import java.util.Optional;

@RestController
@RequestMapping("/v1/administrator/roles")
public class RolesController {

    @Autowired
    private RoleService service;

    @PostMapping("/list")
    public ResponseEntity<Page<RoleEntity>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<RoleEntity> data = service.list(keyword, pageable);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleEntity> get(@PathVariable Long id) {
        Optional<RoleEntity> entity = service.findById(id);
        return ResponseEntity.of(entity);
    }

    @PostMapping("/saveOrUpdate")
    public ResponseEntity<RoleEntity> saveOrUpdate(@RequestBody RoleEntity entity) {
        RoleEntity saved = service.saveOrUpdate(entity);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<RoleEntity> cloneRole(@PathVariable Long id, @RequestBody RoleCloneDTO dto) {
        RoleEntity cloned = service.cloneRole(id, dto.getName(), dto.getDisplayName());
        return ResponseEntity.ok(cloned);
    }
}