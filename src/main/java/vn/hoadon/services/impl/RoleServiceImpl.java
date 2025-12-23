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
import java.util.Collections;
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
    public RoleEntity saveOrUpdate(RoleEntity incoming) {
        // Unique name validation
        if (incoming.getName() == null || incoming.getName().isBlank()) {
            throw new IllegalArgumentException("Tên vai trò không được để trống");
        }

        LocalDateTime now = LocalDateTime.now();
        RoleEntity target;
        boolean isCreate = incoming.getId() == null;

        if (isCreate) {
            repository.findByName(incoming.getName()).ifPresent(x -> {
                throw new IllegalArgumentException("Tên vai trò đã tồn tại");
            });
            target = new RoleEntity();
            target.setCreatedAt(now);
        } else {
            target = repository.findById(incoming.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + incoming.getId()));
            repository.findByName(incoming.getName()).ifPresent(x -> {
                if (!x.getId().equals(target.getId())) {
                    throw new IllegalArgumentException("Tên vai trò đã tồn tại");
                }
            });
        }

        // Basic fields
        target.setName(incoming.getName());
        target.setDisplayName(incoming.getDisplayName());
        target.setDescription(incoming.getDescription());
        target.setUpdatedAt(now);

        // Permissions handling
        if (incoming.getPermissions() != null) {
            if (incoming.getPermissions().isEmpty()) {
                target.setPermissions(Collections.emptySet());
            } else {
                Set<Long> ids = incoming.getPermissions().stream()
                        .map(PermissionEntity::getId)
                        .collect(Collectors.toSet());
                Set<PermissionEntity> managed = ids.stream()
                        .map(id -> permissionRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id)))
                        .collect(Collectors.toSet());
                target.setPermissions(managed);
            }
        }

        return repository.save(target);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public RoleEntity cloneRole(Long id, String name, String displayName) {
        RoleEntity source = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));

        repository.findByName(name).ifPresent(x -> {
            throw new IllegalArgumentException("Tên vai trò đã tồn tại");
        });

        RoleEntity clone = new RoleEntity();
        LocalDateTime now = LocalDateTime.now();
        clone.setName(name);
        clone.setDisplayName(displayName);
        clone.setDescription(source.getDescription());
        clone.setCreatedAt(now);
        clone.setUpdatedAt(now);

        if (source.getPermissions() != null && !source.getPermissions().isEmpty()) {
            Set<Long> ids = source.getPermissions().stream().map(PermissionEntity::getId).collect(Collectors.toSet());
            Set<PermissionEntity> managed = ids.stream()
                    .map(pid -> permissionRepository.findById(pid)
                            .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + pid)))
                    .collect(Collectors.toSet());
            clone.setPermissions(managed);
        }
        return repository.save(clone);
    }
}