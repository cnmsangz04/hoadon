package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.dto.member.MemberUpsertRequest;
import vn.hoadon.entity.PermissionEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.entity.UserPermissionEntity;
import vn.hoadon.repositories.PermissionRepository;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.repositories.UserPermissionRepository;
import vn.hoadon.services.MemberService;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MemberServiceImpl implements MemberService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Page<UserEntity> list(String keyword, Long roleId, Byte status, Long companyId, Integer role, Pageable pageable) {
        Specification<UserEntity> spec = (root, query, cb) -> {
            var preds = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            preds.add(cb.isNull(root.get("deletedAt")));

            if (companyId != null) {
                preds.add(cb.equal(root.get("companyId"), companyId));
            }

            if (companyId != null && companyId != 1L) {
                preds.add(cb.notEqual(root.get("role"), 0));
                preds.add(cb.not(cb.and(
                        cb.equal(root.get("companyId"), 1L),
                        cb.equal(root.get("role"), 1)
                )));
            }

            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("username")), kw),
                        cb.like(cb.lower(root.get("name")), kw),
                        cb.like(cb.lower(root.get("email")), kw)
                ));
            }

            // Drop roleId filter tied to workRole
            if (role != null) {
                preds.add(cb.equal(root.get("role"), role));
            }

            if (status != null) {
                preds.add(cb.equal(root.get("status"), status));
            }

            return cb.and(preds.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return userRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public UserEntity saveOrUpdate(MemberUpsertRequest incoming) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity actor = (auth != null && auth.getPrincipal() instanceof UserEntity)
                ? (UserEntity) auth.getPrincipal()
                : null;

        Integer actorRole = actor != null ? actor.getRole() : null;
        Long actorCompanyId = actor != null ? actor.getCompanyId() : null;
        boolean isRoot = actorRole != null && actorRole == 0;

        // Determine target companyId: only root can set arbitrary companyId; admin/user defaults to actor company
        Long companyId = incoming.getCompanyId();
        if (!isRoot) {
            companyId = actorCompanyId;
        }

        // Validate role changes (if provided)
        Integer role = incoming.getRole();
        if (role != null && role == 0) {
            throw new IllegalArgumentException("Không được gán role root");
        }

        LocalDateTime now = LocalDateTime.now();
        UserEntity user;
        boolean isCreate = incoming.getId() == null;

        if (isCreate) {
            // companyId MUST be present on create (users.company_id NOT NULL)
            if (companyId == null) {
                throw new IllegalArgumentException("Thiếu companyId khi tạo thành viên. Vui lòng chọn công ty hoặc đăng nhập đúng ngữ cảnh công ty.");
            }
            Optional<UserEntity> existed = userRepository.findByUsername(incoming.getUsername());
            if (existed.isPresent()) {
                throw new IllegalArgumentException("Username đã tồn tại");
            }
            user = new UserEntity();
            user.setCreatedAt(now);
            user.setPassword(passwordEncoder.encode(
                    incoming.getPassword() != null ? incoming.getPassword() : "ChangeMe123"
            ));
            // Admin password on create if provided
            if (incoming.getAdminPassword() != null) {
                String ap = incoming.getAdminPassword();
                if (ap.isBlank()) {
                    user.setAdminPassword(null);
                } else {
                    user.setAdminPassword(passwordEncoder.encode(ap));
                }
            }
            // Required fields on create
            user.setCompanyId(companyId);
            user.setUsername(incoming.getUsername());
            user.setRole(role != null ? role : 2);
            user.setStatus(incoming.getStatus() != null ? incoming.getStatus() : (byte) 1);
        } else {
            user = userRepository.findById(incoming.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Only update password if provided
            if (incoming.getPassword() != null && !incoming.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(incoming.getPassword()));
            }
            // Admin password on update if provided
            if (incoming.getAdminPassword() != null) {
                String ap = incoming.getAdminPassword();
                if (ap.isBlank()) {
                    user.setAdminPassword(null);
                } else {
                    user.setAdminPassword(passwordEncoder.encode(ap));
                }
            }

            // Update companyId only when explicitly provided by root; avoid nulling accidentally
            if (isRoot && incoming.getCompanyId() != null) {
                user.setCompanyId(incoming.getCompanyId());
            }

            // Only update role if provided
            if (role != null) {
                user.setRole(role);
            }

            // Only update status if provided
            if (incoming.getStatus() != null) {
                user.setStatus(incoming.getStatus());
            }

            // Only update username if provided and not blank
            if (incoming.getUsername() != null && !incoming.getUsername().isBlank()) {
                Optional<UserEntity> existed = userRepository.findByUsername(incoming.getUsername());
                if (existed.isPresent() && !existed.get().getId().equals(user.getId())) {
                    throw new IllegalArgumentException("Username đã tồn tại");
                }
                user.setUsername(incoming.getUsername());
            }
        }

        // Common optional fields: update only when provided
        if (incoming.getName() != null) {
            user.setName(incoming.getName());
        }
        if (incoming.getEmail() != null) {
            user.setEmail(incoming.getEmail());
        }
        if (incoming.getPhone() != null) {
            user.setPhone(incoming.getPhone());
        }
        if (incoming.getAvatar() != null) {
            user.setAvatar(incoming.getAvatar());
        }

        user.setUpdatedAt(now);

        // Persist user first
        user = userRepository.save(user);

        // Save per-user overrides if any
        if (incoming.getUserPermissions() != null) {
            for (var up : incoming.getUserPermissions()) {
                PermissionEntity p = permissionRepository.findById(up.getPermissionId())
                        .orElseThrow(() -> new IllegalArgumentException("Permission not found"));
                UserPermissionEntity upe =
                        userPermissionRepository.findByUserIdAndPermissionId(user.getId(), p.getId());
                if (upe == null) {
                    upe = new UserPermissionEntity();
                    upe.setUser(user);
                    upe.setPermission(p);
                }
                upe.setAllowed((byte) (up.getAllowed() == 1 ? 1 : 0));
                userPermissionRepository.save(upe);
            }
        }

        return user;
    }

    @Override
    @Transactional
    public void setLock(Long id, boolean lock) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus((byte) (lock ? 0 : 1));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public String resetPassword(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity actor = (auth != null && auth.getPrincipal() instanceof UserEntity)
                ? (UserEntity) auth.getPrincipal()
                : null;
        Integer actorRole = actor != null ? actor.getRole() : null;
        boolean isAdmin = actorRole != null && actorRole == 1;
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // Admin can reset only for self or non-admins
        if (isAdmin) {
            boolean isSelf = actor != null && actor.getId().equals(id);
            boolean targetIsAdmin = user.getRole() != null && user.getRole() == 1;
            if (!isSelf && targetIsAdmin) {
                throw new IllegalArgumentException("Admin chỉ có thể Reset mật khẩu cho chính mình hoặc Nhân viên");
            }
        }
        String newPass = "Reset@" + System.currentTimeMillis();
        user.setPassword(passwordEncoder.encode(newPass));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return newPass;
    }

    @Override
    @Transactional
    public void removeFromCompany(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setCompanyId(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setDeletedAt(LocalDateTime.now());
        user.setStatus((byte) 0);
        user.setCompanyId(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        userPermissionRepository.deleteAllByUserId(user.getId());
    }

    @Override
    public UserEntity getById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}