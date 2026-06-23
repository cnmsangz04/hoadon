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
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.PermissionEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.entity.UserPermissionEntity;
import vn.hoadon.messaging.MailJobMessage;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.PermissionRepository;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.repositories.UserPermissionRepository;
import vn.hoadon.security.UserRoles;
import vn.hoadon.services.MailQueueService;
import vn.hoadon.services.MemberService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class MemberServiceImpl implements MemberService {

    private static final String ACCOUNT_INFO_MAIL_TEMPLATE = "ACCOUNT_INFO_MAIL";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private MailQueueService mailQueueService;

    @Override
    public Page<UserEntity> list(String keyword, Long roleId, Byte status, Long companyId, Integer role, Pageable pageable) {
        Specification<UserEntity> spec = (root, query, cb) -> {
            var preds = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            preds.add(cb.isNull(root.get("deletedAt")));

            if (companyId != null) {
                preds.add(cb.equal(root.get("companyId"), companyId));
            }

            if (companyId != null && companyId != 1L) {
                preds.add(cb.notEqual(root.get("role"), UserRoles.ROOT));
                preds.add(cb.notEqual(root.get("role"), UserRoles.SYSTEM_ADMIN));
            }

            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("username")), kw),
                        cb.like(cb.lower(root.get("name")), kw),
                        cb.like(cb.lower(root.get("email")), kw)
                ));
            }

            // Bỏ bộ lọc roleId gắn với workRole.
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
        boolean isRoot = UserRoles.isRoot(actorRole);

        // Xác định companyId mục tiêu: Quản trị viên toàn quyền có thể truyền rõ công ty, nếu không thì dùng công ty hiện tại.
        Long companyId = incoming.getCompanyId();
        if (!isRoot || companyId == null) {
            companyId = actorCompanyId;
        }

        // Kiểm tra thay đổi vai trò nếu payload có gửi.
        Integer role = incoming.getRole();
        if (role != null && UserRoles.isRoot(role)) {
            throw new IllegalArgumentException("Không được gán vai trò Quản trị viên toàn quyền");
        }
        LocalDateTime now = LocalDateTime.now();
        UserEntity user;
        boolean isCreate = incoming.getId() == null;
        if (!isRoot && isCreate && role != null && !UserRoles.isCompanyStaff(role)) {
            throw new IllegalArgumentException("Chỉ Quản trị viên toàn quyền mới được gán vai trò quản trị/quản lý");
        }

        if (isCreate) {
            // companyId bắt buộc có khi tạo mới (users.company_id NOT NULL)
            if (companyId == null) {
                throw new IllegalArgumentException("Thiếu companyId khi tạo thành viên. Vui lòng chọn công ty hoặc đăng nhập đúng ngữ cảnh công ty.");
            }
            
            // Chỉ kiểm tra username đã tồn tại nếu có truyền username
            if (incoming.getUsername() != null && !incoming.getUsername().isBlank()) {
                Optional<UserEntity> existed = userRepository.findByUsername(incoming.getUsername());
                if (existed.isPresent()) {
                    throw new IllegalArgumentException("Username đã tồn tại");
                }
            }
            
            user = new UserEntity();
            user.setCreatedAt(now);
            
            // Mã hóa mật khẩu đăng nhập người dùng.
            String plainPassword = incoming.getPassword() != null ? incoming.getPassword() : "ChangeMe123";
            user.setPassword(passwordEncoder.encode(plainPassword));
            
            // Các trường bắt buộc khi tạo mới
            user.setCompanyId(companyId);
            // Nếu có username thì dùng, nếu không thì tạo tạm.
            if (incoming.getUsername() != null && !incoming.getUsername().isBlank()) {
                user.setUsername(incoming.getUsername());
            } else {
                // Tạo username tạm, sẽ cập nhật sau khi lưu
                user.setUsername("temp_" + System.currentTimeMillis());
            }
            user.setRole(role != null ? role : UserRoles.COMPANY_STAFF);
            user.setStatus(incoming.getStatus() != null ? incoming.getStatus() : (byte) 1);
        } else {
            user = userRepository.findById(incoming.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            ensureCanManageTarget(actor, user, false);

            // Chỉ cập nhật mật khẩu nếu có truyền
            if (incoming.getPassword() != null && !incoming.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(incoming.getPassword()));
            }
            // Quản trị viên toàn quyền chỉ đổi companyId khi payload gửi rõ companyId.
            if (isRoot && incoming.getCompanyId() != null) {
                user.setCompanyId(incoming.getCompanyId());
            }

            // Chỉ cập nhật role nếu có truyền
            if (role != null) {
                if (!isRoot && !role.equals(user.getRole())) {
                    throw new IllegalArgumentException("Chỉ Quản trị viên toàn quyền mới được thay đổi vai trò");
                }
                user.setRole(role);
            }

            // Chỉ cập nhật status nếu có truyền
            if (incoming.getStatus() != null) {
                user.setStatus(incoming.getStatus());
            }

            // Chỉ cập nhật username nếu có truyền và không rỗng
            if (incoming.getUsername() != null && !incoming.getUsername().isBlank()) {
                Optional<UserEntity> existed = userRepository.findByUsername(incoming.getUsername());
                if (existed.isPresent() && !existed.get().getId().equals(user.getId())) {
                    throw new IllegalArgumentException("Username đã tồn tại");
                }
                user.setUsername(incoming.getUsername());
            }
        }

        // Các trường tùy chọn chỉ cập nhật khi payload có gửi.
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

        validateRoleCompany(user.getRole(), user.getCompanyId());
        normalizeAdminAccess(user, incoming.getAdminPassword());
        user.setUpdatedAt(now);

        // Lưu user trước
        user = userRepository.save(user);

        // Tạo username hệ thống nếu trước đó là tạm (cho user mới không truyền username)
        if (isCreate && user.getUsername() != null && user.getUsername().startsWith("temp_")) {
            String systemUsername = "cp-" + user.getId();
            user.setUsername(systemUsername);
            user = userRepository.save(user);
        }

        // Lưu quyền riêng của từng user nếu có.
        if (incoming.getUserPermissions() != null) {
            if (actor != null && actor.getId() != null && actor.getId().equals(user.getId())) {
                throw new IllegalArgumentException("Không được tự phân quyền cho tài khoản đang đăng nhập");
            }
            Integer targetRole = user.getRole();
            for (var up : incoming.getUserPermissions()) {
                PermissionEntity p = permissionRepository.findById(up.getPermissionId())
                        .orElseThrow(() -> new IllegalArgumentException("Permission not found"));
                int level = p.getLevel() != null ? p.getLevel() : 0;
                if (UserRoles.isSystemAdmin(targetRole) && level == 0) {
                    continue;
                }
                if (UserRoles.isCompanyManager(targetRole)) {
                    continue;
                }
                if (UserRoles.isCompanyStaff(targetRole) && level != 0) {
                    continue;
                }
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

    private void normalizeAdminAccess(UserEntity user, String rawAdminPassword) {
        if (user == null) return;

        Integer role = user.getRole();
        boolean canAccessAdminArea = UserRoles.canAccessAdminArea(role);

        if (canAccessAdminArea) {
            if (rawAdminPassword != null) {
                user.setAdminPassword(rawAdminPassword.isBlank() ? null : passwordEncoder.encode(rawAdminPassword));
            }
            return;
        }

        user.setAdminPassword(null);
    }

    private void validateRoleCompany(Integer role, Long companyId) {
        if (UserRoles.isSystemAdmin(role) && !Long.valueOf(1L).equals(companyId)) {
            throw new IllegalArgumentException("Quản trị viên hệ thống phải thuộc công ty root");
        }
    }

    @Override
    @Transactional
    public void setLock(Long id, boolean lock) {
        UserEntity actor = currentActor();
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ensureCanManageTarget(actor, user, false);
        if (actor != null && actor.getId() != null && actor.getId().equals(user.getId())) {
            throw new IllegalArgumentException("Không được tự khóa/mở khóa chính mình");
        }
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
        boolean isCompanyManager = UserRoles.isCompanyManager(actorRole);
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ensureCanManageTarget(actor, user, true);
        // Quản lý doanh nghiệp chỉ được reset mật khẩu của chính mình hoặc nhân viên.
        if (isCompanyManager) {
            boolean isSelf = actor != null && actor.getId().equals(id);
            boolean targetIsManagerOrAbove = user.getRole() != null && user.getRole() <= UserRoles.COMPANY_MANAGER;
            if (!isSelf && targetIsManagerOrAbove) {
                throw new IllegalArgumentException("Quản lý doanh nghiệp chỉ có thể Reset mật khẩu cho chính mình hoặc Nhân viên doanh nghiệp");
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
        UserEntity actor = currentActor();
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ensureCanManageTarget(actor, user, false);
        user.setCompanyId(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        UserEntity actor = currentActor();
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ensureCanManageTarget(actor, user, false);
        user.setDeletedAt(LocalDateTime.now());
        user.setStatus((byte) 0);
        user.setCompanyId(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        userPermissionRepository.deleteAllByUserId(user.getId());
    }

    @Override
    public UserEntity getById(Long id) {
        UserEntity actor = currentActor();
        UserEntity user = userRepository.findById(id).orElse(null);
        if (user == null) return null;
        ensureCanManageTarget(actor, user, true);
        return user;
    }

    @Override
    @Transactional
    public String sendCredentials(Long id) {
        UserEntity actor = currentActor();
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ensureCanManageTarget(actor, user, false);

        String toEmail = user.getEmail();
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("Thành viên chưa có email nhận thông tin đăng nhập");
        }

        CompanyEntity company = null;
        if (user.getCompanyId() != null) {
            company = companyRepository.findById(user.getCompanyId()).orElse(null);
        }

        // Luôn reset lại mật khẩu mới mỗi lần gửi thông tin
        String newPassword = resetPassword(id);
        // Tải lại user sau khi reset mật khẩu
        user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        enqueueAccountInfoMail(user, company, toEmail, newPassword);

        userRepository.save(user);
        
        return newPassword;
    }

    private void enqueueAccountInfoMail(UserEntity user, CompanyEntity company, String toEmail, String rawPassword) {
        if (user == null || toEmail == null || toEmail.isBlank()) return;

        String memberName = firstNotBlank(user.getName(), user.getUsername(), toEmail);
        String companyName = company != null ? firstNotBlank(company.getName(), company.getContactName(), "") : "";

        Map<String, String> vars = new HashMap<>();
        vars.put("SUBJECT", "Thông tin đăng nhập tài khoản");
        vars.put("NAME", memberName);
        vars.put("CUS_NAME", vars.get("NAME"));
        vars.put("COMPANY", companyName);
        vars.put("CUS_COM_NAME", vars.get("COMPANY"));
        vars.put("COM_NAME", resolveLoginMailSenderCompanyName());
        vars.put("LINK", "http://localhost:8080/login");
        vars.put("USERNAME", user.getUsername() != null ? user.getUsername() : "");
        vars.put("PASSWORD", rawPassword != null ? rawPassword : "");

        MailJobMessage msg = new MailJobMessage();
        msg.setTemplateKey(ACCOUNT_INFO_MAIL_TEMPLATE);
        msg.setCompanyId(user.getCompanyId());
        msg.setToEmail(toEmail.trim());
        msg.setToName(vars.get("NAME"));
        msg.setVariables(vars);
        mailQueueService.enqueue(msg);
    }

    private String firstNotBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String resolveLoginMailSenderCompanyName() {
        return companyRepository.findById(1L)
                .map(CompanyEntity::getName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("");
    }

    private UserEntity currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getPrincipal() instanceof UserEntity)
                ? (UserEntity) auth.getPrincipal()
                : null;
    }

    private void ensureCanManageTarget(UserEntity actor, UserEntity target, boolean allowSelf) {
        if (actor == null || target == null) {
            throw new IllegalArgumentException("Không xác định được người thao tác");
        }
        Integer actorRole = actor.getRole();
        if (UserRoles.isRoot(actorRole)) {
            return;
        }
        if (allowSelf && actor.getId() != null && actor.getId().equals(target.getId())) {
            return;
        }
        if (actor.getCompanyId() == null || target.getCompanyId() == null
                || !actor.getCompanyId().equals(target.getCompanyId())) {
            throw new IllegalArgumentException("Không được thao tác thành viên của công ty khác");
        }
        if (UserRoles.isRoot(target.getRole())) {
            throw new IllegalArgumentException("Không được thao tác tài khoản Quản trị viên toàn quyền");
        }
        boolean isSelf = actor.getId() != null && actor.getId().equals(target.getId());
        Integer targetRole = target.getRole();
        if (UserRoles.isSystemAdmin(actorRole) && UserRoles.isSystemAdmin(targetRole) && !isSelf) {
            throw new IllegalArgumentException("Quản trị viên hệ thống không được thao tác tài khoản Quản trị viên hệ thống khác");
        }
        if (UserRoles.isCompanyManager(actorRole) && targetRole != null && targetRole <= UserRoles.COMPANY_MANAGER && !isSelf) {
            throw new IllegalArgumentException("Quản lý doanh nghiệp không được thao tác tài khoản quản trị/quản lý khác");
        }
        if (UserRoles.isCompanyStaff(actorRole) && !UserRoles.isCompanyStaff(targetRole)) {
            throw new IllegalArgumentException("Nhân viên doanh nghiệp chỉ được thao tác tài khoản nhân viên cùng công ty");
        }
    }
}
