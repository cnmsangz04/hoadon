package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.entity.PermissionEntity;
import vn.hoadon.entity.RoleEntity;
import vn.hoadon.repositories.PermissionRepository;
import vn.hoadon.repositories.RoleRepository;
import vn.hoadon.services.RoleService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository repository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public Page<RoleEntity> list(String keyword, Pageable pageable) {
        Specification<RoleEntity> spec = (root, query, cb) -> {
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                return cb.or(
                        cb.like(root.get("name"), like),
                        cb.like(root.get("displayName"), like)
                );
            }
            return cb.conjunction();
        };
        return repository.findAll(spec, pageable);
    }

    @Override
    public Optional<RoleEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public RoleEntity saveOrUpdate(RoleEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        if (entity.getId() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        // If permissions are provided with only IDs, ensure managed entities
        if (entity.getPermissions() != null && !entity.getPermissions().isEmpty()) {
            Set<Long> ids = entity.getPermissions().stream()
                    .map(PermissionEntity::getId)
                    .collect(Collectors.toSet());
            Set<PermissionEntity> managed = ids.stream()
                    .map(id -> permissionRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id)))
                    .collect(Collectors.toSet());
            entity.setPermissions(managed);
        }
        return repository.save(entity);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
