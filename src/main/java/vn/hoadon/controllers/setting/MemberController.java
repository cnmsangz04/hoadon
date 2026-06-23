package vn.hoadon.controllers.setting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.PermissionCategoryEntity;
import vn.hoadon.entity.PermissionEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.dto.member.MemberUpsertRequest;
import vn.hoadon.services.MemberService;
import vn.hoadon.repositories.PermissionCategoryRepository;
import vn.hoadon.repositories.PermissionRepository;
import vn.hoadon.repositories.UserPermissionRepository;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.security.UserRoles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/setting/members")
public class MemberController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(MemberController.class);

    @Autowired
    private MemberService service;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PermissionCategoryRepository permissionCategoryRepository;

    @GetMapping("/abilities")
    public Map<String, Object> abilities() {
        UserEntity user = currentUser();
        Map<String, Object> res = new HashMap<>();
        res.put("canList", permissionService.hasPermission(user, "setting-member-list", false, true));
        res.put("canSave", permissionService.hasPermission(user, "setting-member-save", false, true));
        res.put("canManage", permissionService.hasPermission(user, "setting-member-manage", false, true));
        res.put("role", user != null ? user.getRole() : null);
        res.put("companyId", user != null ? user.getCompanyId() : null);
        return res;
    }

    @PostMapping("/list")
    public Map<String, Object> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Byte status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Integer role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        permission("setting-member-list");
        // Suy ra companyId từ user đã xác thực cho user không phải Quản trị viên toàn quyền
        UserEntity user = currentUser();
        Long actorCompanyId = user != null ? user.getCompanyId() : null;
        Integer actorRole = user != null ? user.getRole() : null;
        boolean isRoot = UserRoles.isRoot(actorRole);
        if (!isRoot || companyId == null) {
            companyId = actorCompanyId;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> p = service.list(keyword, roleId, status, companyId, role, pageable);
        return toPaginationResponse(p);
    }

    @GetMapping("/list")
    public Map<String, Object> listGet(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Byte status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Integer role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        permission("setting-member-list");
        // Suy ra companyId từ user đã xác thực cho user không phải Quản trị viên toàn quyền
        UserEntity user = currentUser();
        Long actorCompanyId = user != null ? user.getCompanyId() : null;
        Integer actorRole = user != null ? user.getRole() : null;
        boolean isRoot = UserRoles.isRoot(actorRole);
        if (!isRoot || companyId == null) {
            companyId = actorCompanyId;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> p = service.list(keyword, roleId, status, companyId, role, pageable);
        return toPaginationResponse(p);
    }

    private Map<String, Object> toPaginationResponse(Page<UserEntity> p) {
        Map<String, Object> res = new HashMap<>();
        long total = p.getTotalElements();
        int size = p.getSize();
        int currentPage = p.getNumber() + 1; // bắt đầu từ 1
        int lastPage = Math.max(1, p.getTotalPages());
        int numberOfElements = p.getNumberOfElements();
        long from = total == 0 ? 0 : ((long) (currentPage - 1) * size) + 1;
        long to = total == 0 ? 0 : (from + numberOfElements - 1);

        res.put("data", p.getContent().stream().map(this::toMemberListItem).collect(Collectors.toList()));
        res.put("total", total);
        res.put("per_page", size);
        res.put("current_page", currentPage);
        res.put("last_page", lastPage);
        res.put("from", from);
        res.put("to", to);
        // URL là tùy chọn; tạm giữ null
        res.put("prev_page_url", currentPage > 1 ? currentPage - 1 : null);
        res.put("next_page_url", currentPage < lastPage ? currentPage + 1 : null);
        return res;
    }

    private MemberListItemDto toMemberListItem(UserEntity user) {
        MemberListItemDto dto = new MemberListItemDto();
        if (user == null) {
            return dto;
        }
        dto.setId(user.getId());
        dto.setCompanyId(user.getCompanyId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        return dto;
    }

    @PostMapping("/saveOrUpdate")
    public UserEntity saveOrUpdate(@RequestBody MemberUpsertRequest incoming) {
        permission("setting-member-save");
        UserEntity actor = currentUser();
        Integer actorRole = actor != null ? actor.getRole() : null;
        boolean isRoot = UserRoles.isRoot(actorRole);
        
        // Chỉ ghi đè companyId cho user không phải Quản trị viên toàn quyền
        // Quản trị viên toàn quyền (role=0) có thể quản lý user từ mọi công ty
        if (!isRoot) {
            Long companyId = actor != null ? actor.getCompanyId() : null;
            if (companyId != null) {
                incoming.setCompanyId(companyId);
            }
        }
        
        return service.saveOrUpdate(incoming);
    }

    @GetMapping("/{id}/permissions")
    public List<MemberPermissionDto> getUserPermissions(@PathVariable Long id) {
        permission("setting-member-save");
        UserEntity target = service.getById(id);
        if (target == null) {
            return List.of();
        }
        return userPermissionRepository.findByUserId(id).stream().map(upe -> {
            MemberPermissionDto dto = new MemberPermissionDto();
            dto.setPermissionId(upe.getPermission().getId());
            dto.setAllowed((int) (upe.getAllowed() != null ? upe.getAllowed() : 0));
            return dto;
        }).collect(Collectors.toList());
    }

    @GetMapping("/permission-catalog")
    public List<PermissionEntity> permissionCatalog() {
        permission("setting-member-save");
        return permissionRepository.findAll(Sort.by(
                Sort.Order.asc("level"),
                Sort.Order.asc("name")
        ));
    }

    @GetMapping("/permission-categories")
    public List<PermissionCategoryEntity> permissionCategories() {
        permission("setting-member-save");
        return permissionCategoryRepository.findAll(Sort.by(
                Sort.Order.asc("orderIndex"),
                Sort.Order.asc("id")
        ));
    }

    public static class MemberPermissionDto {
        private Long permissionId;
        private Integer allowed;
        public Long getPermissionId() { return permissionId; }
        public void setPermissionId(Long permissionId) { this.permissionId = permissionId; }
        public Integer getAllowed() { return allowed; }
        public void setAllowed(Integer allowed) { this.allowed = allowed; }
    }

    public static class MemberListItemDto {
        private Long id;
        private Long companyId;
        private String username;
        private String name;
        private String email;
        private String phone;
        private String avatar;
        private Integer role;
        private Byte status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getCompanyId() { return companyId; }
        public void setCompanyId(Long companyId) { this.companyId = companyId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public Integer getRole() { return role; }
        public void setRole(Integer role) { this.role = role; }
        public Byte getStatus() { return status; }
        public void setStatus(Byte status) { this.status = status; }
    }

    @PostMapping("/{id}/lock")
    public void lock(@PathVariable Long id, @RequestParam("lock") int lock) {
        permission("setting-member-manage");
        service.setLock(id, lock == 1);
    }

    @PostMapping("/{id}/reset-password")
    public Map<String, Object> resetPassword(@PathVariable Long id) {
        permission("setting-member-manage");
        String tempPassword = service.resetPassword(id);
        
        // Ghi log reset mật khẩu kèm chi tiết user
        UserEntity user = userRepository.findById(id).orElse(null);
        if (user != null) {
            log.info("Password reset for userId={}, username={}, email={}, newPassword={}", 
                     id, user.getUsername(), user.getEmail(), tempPassword);
        } else {
            log.info("Password reset for userId={}, newPassword={}", id, tempPassword);
        }
        
        Map<String, Object> res = new HashMap<>();
        res.put("password", tempPassword);
        res.put("message", "Đã reset mật khẩu thành công");
        return res;
    }

    @DeleteMapping("/{id}")
    public void removeFromCompany(@PathVariable Long id) {
        permission("setting-member-manage");
        // Thực hiện soft-delete cho thành viên
        service.delete(id);
    }

    @PostMapping("/{id}/send-credentials")
    public Map<String, Object> sendCredentials(@PathVariable Long id) {
        permission("setting-member-manage");
        // Chỉ cho phép user không phải guest; có thể thêm kiểm tra role chi tiết nếu cần
        UserEntity actor = currentUser();
        if (actor == null) {
            throw new RuntimeException("Unauthenticated");
        }
        
        // Lấy thông tin user trước khi gửi
        UserEntity user = service.getById(id);
        String username = user != null ? user.getUsername() : "unknown";
        String email = user != null ? user.getEmail() : "unknown";
        
        // Gửi thông tin đăng nhập (sẽ reset mật khẩu nội bộ và trả về mật khẩu)
        String newPassword = service.sendCredentials(id);
        
        // Ghi log tài khoản và mật khẩu được gửi
        log.info("Sending credentials to userId={}, username={}, email={}, newPassword={} (by actorId={}, actorUsername={})", 
                 id, username, email, newPassword, actor.getId(), actor.getUsername());
        
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Đã gửi thông tin tài khoản tới email thành viên");
        return res;
    }
}
