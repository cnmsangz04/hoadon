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
import vn.hoadon.entity.UserPermissionEntity;
import vn.hoadon.repositories.PermissionCategoryRepository;
import vn.hoadon.repositories.PermissionRepository;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.repositories.UserPermissionRepository;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

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

        // Auto-assign to all root users (role=0)
        try {
            List<UserEntity> roots = userRepository.findByRole(0);
            if (roots != null && !roots.isEmpty()) {
                for (UserEntity u : roots) {
                    if (u == null || u.getId() == null) continue;
                    UserPermissionEntity existing = userPermissionRepository.findByUserIdAndPermissionId(u.getId(), saved.getId());
                    if (existing == null) {
                        UserPermissionEntity up = new UserPermissionEntity();
                        up.setUser(u);
                        up.setPermission(saved);
                        up.setAllowed((byte)1);
                        userPermissionRepository.save(up);
                    } else {
                        // ensure allowed
                        existing.setAllowed((byte)1);
                        userPermissionRepository.save(existing);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Failed assigning permission to roots: {}", ex.getMessage());
        }

        // Nếu level = 0, gán thêm cho toàn bộ admin (role = 1)
        int lvl = saved.getLevel() != null ? saved.getLevel() : 2;
        if (lvl == 0) {
            try {
                List<UserEntity> admins = userRepository.findByRole(1);
                if (admins != null && !admins.isEmpty()) {
                    for (UserEntity u : admins) {
                        if (u == null || u.getId() == null) continue;
                        UserPermissionEntity existing = userPermissionRepository.findByUserIdAndPermissionId(u.getId(), saved.getId());
                        if (existing == null) {
                            UserPermissionEntity up = new UserPermissionEntity();
                            up.setUser(u);
                            up.setPermission(saved);
                            up.setAllowed((byte)1);
                            userPermissionRepository.save(up);
                        } else {
                            existing.setAllowed((byte)1);
                            userPermissionRepository.save(existing);
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed assigning permission to admins: {}", ex.getMessage());
            }
        }

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

        // User tồn tại?
        if (user == null) {
            log.warn("Permission denied: user is null");
            return deny(isBoolean);
        }

        log.debug("Check permission for user={}, role={}, keys={}, requireAll={}",
                user.getUsername(),
                user.getRole(),
                keys,
                requireAll);

        // Root? (Super admin)
        if (user.getRole() != null && user.getRole() == 0) {
            log.debug("Super admin detected → allow all");
            return true;
        }

        // keys validation
        if (keys == null || keys.isBlank()) {
            log.warn("Permission denied: keys missing");
            return deny(isBoolean);
        }

        // Chuẩn hóa key đầu vào
        List<String> permissionKeys = Arrays.stream(keys.split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList();

        // Compute user's max level from effective permissions set
        int userMaxLevel = 0;
        try {
            if (user.getPermissions() != null && !user.getPermissions().isEmpty()) {
                for (PermissionEntity p : user.getPermissions()) {
                    if (p != null && p.getLevel() != null) {
                        userMaxLevel = Math.max(userMaxLevel, p.getLevel());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not compute userMaxLevel: {}", e.getMessage());
        }
        final int maxLevel = userMaxLevel;

        // Checker for a single permission key according to the required order
        java.util.function.Function<String, Boolean> checkOne = (String key) -> {
            // Lấy permission theo key
            Optional<PermissionEntity> permOpt = permissionRepository.findByName(key);
            if (permOpt.isEmpty()) {
                log.warn("Permission key not found: {} (config sai)", key);
                return false; // config sai → deny this key
            }
            PermissionEntity perm = permOpt.get();

            // Check level: user.maxLevel >= permission.level ?
            int level = perm.getLevel() != null ? perm.getLevel() : 0;
            if (maxLevel < level) {
                log.warn("Permission denied by level: userMaxLevel={}, requiredLevel={}, key={}", maxLevel, level, key);
                return false;
            }

            // Check override (user_permissions)
            // allowed = 1 → PASS, allowed = 0 → DENY
            if (user.getUserPermissionOverrides() != null) {
                for (UserPermissionEntity ov : user.getUserPermissionOverrides()) {
                    if (ov == null || ov.getPermission() == null) continue;
                    PermissionEntity ovPerm = ov.getPermission();
                    if (ovPerm.getName() != null && ovPerm.getName().equals(key)) {
                        byte allowed = ov.getAllowed() != null ? ov.getAllowed() : (byte)0;
                        if (allowed == 1) {
                            log.debug("Override allow found for key={}", key);
                            return true; // PASS immediately
                        } else {
                            log.warn("Override deny found for key={}", key);
                            return false; // DENY immediately
                        }
                    }
                }
            }

            // Drop role-based permission check (work_role)
            log.warn("Permission not granted by override for key={}", key);
            return false;
        };

        boolean finalAllowed;
        if (requireAll) {
            finalAllowed = permissionKeys.stream().allMatch(checkOne::apply);
        } else {
            finalAllowed = permissionKeys.stream().anyMatch(checkOne::apply);
        }

        if (finalAllowed) {
            log.debug("Permission granted for user={}, keys={} (requireAll={})", user.getUsername(), permissionKeys, requireAll);
            return true;
        }

        log.warn("Permission denied for user={}, keys={} (requireAll={})", user.getUsername(), permissionKeys, requireAll);
        return deny(isBoolean);
    }

    private boolean deny(boolean isBoolean) {
        if (isBoolean) {
            return false;
        }
        throw new AccessDeniedException("Bạn không có quyền thao tác cho hành động này");
    }
}