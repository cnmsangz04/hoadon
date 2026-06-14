package vn.hoadon.controllers.admin;

import vn.hoadon.controllers.base.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.entity.PermissionCategoryEntity;
import vn.hoadon.services.PermissionCategoryService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/administrator/permission-categories")
public class PermissionCategoriesController extends BaseController {

    @Autowired
    private PermissionCategoryService service;

    @PostMapping("/list")
    public ResponseEntity<Page<PermissionCategoryEntity>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        permission("permission-category-manage");
        Pageable pageable = PageRequest.of(page, size, Sort.by(
                // Sắp xếp ưu tiên theo sothutu (orderIndex) tăng dần
                Sort.Order.asc("orderIndex"),
                // Nếu cùng sothutu thì mới xét đến createdAt (mới nhất trước)
                Sort.Order.desc("createdAt")
        ));
        Page<PermissionCategoryEntity> data = service.list(keyword, pageable);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermissionCategoryEntity> get(@PathVariable Long id) {
        permission("permission-category-manage");
        Optional<PermissionCategoryEntity> entity = service.findById(id);
        return ResponseEntity.of(entity);
    }

    @PostMapping("/saveOrUpdate")
    public ResponseEntity<PermissionCategoryEntity> saveOrUpdate(@RequestBody PermissionCategoryEntity entity) {
        permission("permission-category-manage");
        PermissionCategoryEntity saved = service.saveOrUpdate(entity);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permission("permission-category-manage");
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reorder")
    public ResponseEntity<Void> reorder(@RequestBody List<Long> orderedIds) {
        permission("permission-category-manage");
        service.reorder(orderedIds);
        return ResponseEntity.ok().build();
    }
}