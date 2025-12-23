package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.entity.PermissionEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.PermissionRepository;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.services.MemberService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MemberServiceImpl implements MemberService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Page<UserEntity> list(String keyword, Long roleId, Byte status, Pageable pageable) {
        Specification<UserEntity> spec = (root, query, cb) -> {
            // filter by companyId if needed (assume comes from security context)
            // ...add companyId predicate here when security is integrated...
            var predicates = cb.conjunction();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.getExpressions().add(cb.or(
                        cb.like(root.get("username"), like),
                        cb.like(root.get("name"), like),
                        cb.like(root.get("email"), like)
                ));
            }
            if (roleId != null) {
                predicates.getExpressions().add(cb.equal(root.get("role"), roleId.intValue()));
            }
            if (status != null) {
                predicates.getExpressions().add(cb.equal(root.get("status"), status));
            }
            return predicates;
        };
        return userRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public UserEntity saveOrUpdate(UserEntity incoming) {
        // prevent changing to root/admin by role id = 0/1 (assumption)
        if (incoming.getRole() != null) {
            int role = incoming.getRole();
            if (role == 0 || role == 1) {
                throw new IllegalArgumentException("Không được đổi role root/admin");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        UserEntity target;
        boolean isCreate = incoming.getId() == null;
        if (isCreate) {
            target = new UserEntity();
            target.setCreatedAt(now);
            // set default password (must reset later)
            if (incoming.getPassword() == null || incoming.getPassword().isBlank()) {
                target.setPassword(passwordEncoder.encode("ChangeMe123"));
            } else {
                target.setPassword(passwordEncoder.encode(incoming.getPassword()));
            }
        } else {
            target = userRepository.findById(incoming.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + incoming.getId()));
        }
        // Basic fields
        target.setUsername(incoming.getUsername());
        target.setName(incoming.getName());
        target.setEmail(incoming.getEmail());
        target.setAvatar(incoming.getAvatar());
        target.setRole(incoming.getRole());
        target.setPhone(incoming.getPhone());
        target.setStatus(incoming.getStatus() != null ? incoming.getStatus() : 1);
        target.setUpdatedAt(now);

        // Extra permissions handling (user_permissions)
        if (incoming.getPermissions() != null) {
            if (incoming.getPermissions().isEmpty()) {
                target.setPermissions(Collections.emptySet());
            } else {
                Set<Long> ids = incoming.getPermissions().stream().map(PermissionEntity::getId).collect(Collectors.toSet());
                Set<PermissionEntity> managed = ids.stream()
                        .map(pid -> permissionRepository.findById(pid)
                                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + pid)))
                        .collect(Collectors.toSet());
                target.setPermissions(managed);
            }
        }

        return userRepository.save(target);
    }

    @Override
    @Transactional
    public void setLock(Long id, boolean lock) {
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setStatus((byte) (lock ? 0 : 1));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long id) {
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        String newPass = "Reset@" + System.currentTimeMillis();
        user.setPassword(passwordEncoder.encode(newPass));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        // TODO: send email/notification with reset token or temporary password
    }

    @Override
    @Transactional
    public void removeFromCompany(Long id) {
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setCompanyId(null); // detach from company, keep user record
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
