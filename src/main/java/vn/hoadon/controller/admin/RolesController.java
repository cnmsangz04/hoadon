package vn.hoadon.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.dto.role.RoleCloneDTO;
import vn.hoadon.entity.RoleEntity;
import vn.hoadon.entity.PermissionEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.services.RoleService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        // Apply backend role visibility enforcement based on current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity actor = null;
        if (auth != null && auth.getPrincipal() instanceof UserEntity) {
            actor = (UserEntity) auth.getPrincipal();
        }

        if (actor != null) {
            Integer accountRole = actor.getRole(); // 0=root, 1=admin, 2=user
            Byte adminType = actor.getAdminType(); // 1=system admin, 2=company admin
            boolean isRoot = accountRole != null && accountRole == 0;
            boolean isSystemAdmin = accountRole != null && accountRole == 1 && adminType != null && adminType == 1;

            if (!isRoot && !isSystemAdmin) {
                // Admin company or lower: only roles whose permissions all have level==0 (or no permissions)
                List<RoleEntity> filtered = data.getContent().stream()
                        .filter(r -> {
                            var perms = r.getPermissions();
                            if (perms == null || perms.isEmpty()) return true; // roles without permissions are allowed
                            return perms.stream().allMatch(p -> {
                                Integer lvl = p.getLevel();
                                return lvl == null || lvl == 0;
                            });
                        })
                        .collect(Collectors.toList());
                Page<RoleEntity> filteredPage = new PageImpl<>(filtered, pageable, filtered.size());
                return ResponseEntity.ok(filteredPage);
            }
        }

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