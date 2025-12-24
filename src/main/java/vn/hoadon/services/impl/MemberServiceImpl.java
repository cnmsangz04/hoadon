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
import vn.hoadon.dto.MemberUpsertRequest;
import vn.hoadon.entity.PermissionEntity;
import vn.hoadon.entity.RoleEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.entity.UserPermissionEntity;
import vn.hoadon.repositories.PermissionRepository;
import vn.hoadon.repositories.RoleRepository;
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
    private RoleRepository roleRepository;

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

            if (roleId != null) {
                preds.add(cb.equal(root.get("workRole").get("id"), roleId));
            }

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
        Byte actorAdminType = actor != null ? actor.getAdminType() : null; // 1: system admin, 2: company admin
        Long actorCompanyId = actor != null ? actor.getCompanyId() : null;
        boolean isRoot = actorRole != null && actorRole == 0;
        boolean isSystemAdmin = actorRole != null && actorRole == 1 && actorAdminType != null && actorAdminType == 1;
        boolean isAdminCompany = actorRole != null && actorRole == 1 && actorAdminType != null && actorAdminType == 2;

        Long companyId = incoming.getCompanyId();
        if (!isRoot && !isSystemAdmin) {
            companyId = actorCompanyId;
        }

        Integer role = incoming.getRole();
        if (role != null) {
            if (role == 0) {
                throw new IllegalArgumentException("Không được gán role root");
            }
            if (!isRoot && !isSystemAdmin && role != 1 && role != 2) {
                throw new IllegalArgumentException("Không đủ quyền gán role");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        UserEntity user;
        boolean isCreate = incoming.getId() == null;

        if (isCreate) {
            Optional<UserEntity> existed = userRepository.findByUsername(incoming.getUsername());
            if (existed.isPresent()) {
                throw new IllegalArgumentException("Username đã tồn tại");
            }
            user = new UserEntity();
            user.setCreatedAt(now);
            user.setPassword(passwordEncoder.encode(
                    incoming.getPassword() != null ? incoming.getPassword() : "ChangeMe123"
            ));
        } else {
            user = userRepository.findById(incoming.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (incoming.getPassword() != null && !incoming.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(incoming.getPassword()));
            }
        }

        user.setCompanyId(companyId);
        user.setUsername(incoming.getUsername());
        user.setName(incoming.getName());
        user.setEmail(incoming.getEmail());
        user.setPhone(incoming.getPhone());
        user.setAvatar(incoming.getAvatar());
        user.setRole(role != null ? role : 2);
        user.setStatus(incoming.getStatus() != null ? incoming.getStatus() : (byte) 1);
        user.setUpdatedAt(now);

        // Validate and set work role (vai trò làm việc)
        if (incoming.getWorkRole() != null && incoming.getWorkRole().getId() != null) {
            RoleEntity wr = roleRepository.findById(incoming.getWorkRole().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Work role not found"));

            // If actor is admin company (role=1, admin_type=2), only allow assigning roles whose permissions all have level=0
            if (isAdminCompany) {
                var perms = wr.getPermissions();
                boolean allowed = (perms == null || perms.isEmpty()) || perms.stream().allMatch(p -> {
                    Integer lvl = p.getLevel();
                    return lvl == null || lvl == 0;
                });
                if (!allowed) {
                    throw new IllegalArgumentException("Admin company chỉ được gán vai trò có quyền level=0");
                }
            }

            user.setWorkRole(wr);
        }

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
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
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
        user.setWorkRole(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        userPermissionRepository.deleteAllByUserId(user.getId());
    }

    @Override
    public UserEntity getById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}