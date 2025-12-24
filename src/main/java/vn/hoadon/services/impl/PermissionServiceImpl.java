package vn.hoadon.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoadon.dto.permission.PermissionCreateDTO;
import vn.hoadon.entity.PermissionCategoryEntity;
import vn.hoadon.entity.PermissionEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.PermissionCategoryRepository;
import vn.hoadon.repositories.PermissionRepository;
import vn.hoadon.services.PermissionService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    private static final Logger log =
            LoggerFactory.getLogger(PermissionServiceImpl.class);

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PermissionCategoryRepository categoryRepository;

    @Override
    public Page<PermissionEntity> list(String keyword, Pageable pageable) {

        Specification<PermissionEntity> spec = (root, query, cb) -> {
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                return cb.or(
                        cb.like(root.get("name"), like),
                        cb.like(root.get("displayName"), like)
                );
            }
            return cb.conjunction();
        };

        Page<PermissionEntity> page = permissionRepository.findAll(spec, pageable);

        return page;
    }

    @Override
    public Optional<PermissionEntity> findById(Long id) {
        return permissionRepository.findById(id);
    }

    @Override
    @Transactional
    public PermissionEntity saveOrUpdate(PermissionCreateDTO dto) {

        PermissionEntity entity;
        LocalDateTime now = LocalDateTime.now();

        if (dto.getId() != null) {
            entity = permissionRepository.findById(dto.getId())
                    .orElseGet(() -> {
                        return new PermissionEntity();
                    });
        } else {
            entity = new PermissionEntity();
            entity.setCreatedAt(now);
        }

        entity.setName(dto.getName());
        entity.setDisplayName(dto.getDisplayName());
        entity.setLevel(dto.getLevel() != null ? dto.getLevel() : 2);
        entity.setDescription(dto.getDescription());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());

        if (dto.getCategory() == null) {
            throw new IllegalArgumentException("category is required");
        }

        PermissionCategoryEntity category = categoryRepository
                .findById(dto.getCategory())
                .orElseThrow(() -> {
                    return new IllegalArgumentException("Permission category not found");
                });

        entity.setCategory(category);
        entity.setUpdatedAt(now);

        PermissionEntity saved = permissionRepository.save(entity);

        return saved;
    }

    @Override
    public void delete(Long id) {
        // Soft delete: mark hidden instead of physical delete
        permissionRepository.findById(id).ifPresent(e -> {
            e.setStatus((byte)0);
            e.setUpdatedAt(LocalDateTime.now());
            permissionRepository.save(e);
        });
    }

    @Override
    public void updateStatus(Long id, byte status) {
        permissionRepository.findById(id).ifPresent(e -> {
            e.setStatus(status);
            e.setUpdatedAt(LocalDateTime.now());
            permissionRepository.save(e);
        });
    }

    @Override
    public boolean hasPermission(
            UserEntity user,
            String keys,
            boolean requireAll,
            boolean isBoolean
    ) {

        if (user == null) {
            log.warn("Permission denied: user is null");
            return deny(isBoolean);
        }

        log.debug("Check permission for user={}, role={}, keys={}, requireAll={}",
                user.getUsername(),
                user.getRole(),
                keys,
                requireAll);

        // Super admin
        if (user.getRole() != null && user.getRole() == 0) {
            log.debug("Super admin detected → allow all");
            return true;
        }

        if (user.getPermissions() == null || keys == null || keys.isBlank()) {
            log.warn("Permission denied: permissions or keys missing");
            return deny(isBoolean);
        }

        List<String> permissionKeys = Arrays.asList(keys.split("\\|"));

        Set<String> userPermissions = user.getPermissions()
                .stream()
                .map(PermissionEntity::getName)
                .collect(Collectors.toSet());

        boolean allowed;

        if (requireAll) {
            allowed = permissionKeys.stream()
                    .allMatch(userPermissions::contains);
        } else {
            allowed = permissionKeys.stream()
                    .anyMatch(userPermissions::contains);
        }

        if (allowed) {
            log.debug("Permission granted for user={}", user.getUsername());
            return true;
        }

        log.warn("Permission denied for user={}, required={}, userHas={}",
                user.getUsername(),
                permissionKeys,
                userPermissions);

        return deny(isBoolean);
    }

    private boolean deny(boolean isBoolean) {
        if (isBoolean) {
            return false;
        }
        throw new AccessDeniedException("Bạn không có quyền thao tác cho hành động này");
    }
}