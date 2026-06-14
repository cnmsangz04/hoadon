package vn.hoadon.controllers.setting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.dto.member.MemberUpsertRequest;
import vn.hoadon.services.MemberService;
import vn.hoadon.repositories.UserPermissionRepository;
import vn.hoadon.repositories.UserRepository;

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
        // Suy ra companyId từ user đã xác thực cho user không phải root
        UserEntity user = currentUser();
        Long actorCompanyId = user != null ? user.getCompanyId() : null;
        Integer actorRole = user != null ? user.getRole() : null;
        boolean isRoot = actorRole != null && actorRole == 0;
        if (!isRoot) {
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
        // Suy ra companyId từ user đã xác thực cho user không phải root
        UserEntity user = currentUser();
        Long actorCompanyId = user != null ? user.getCompanyId() : null;
        Integer actorRole = user != null ? user.getRole() : null;
        boolean isRoot = actorRole != null && actorRole == 0;
        if (!isRoot) {
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

        res.put("data", p.getContent());
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

    @PostMapping("/saveOrUpdate")
    public UserEntity saveOrUpdate(@RequestBody MemberUpsertRequest incoming) {
        permission("setting-member-save");
        UserEntity actor = currentUser();
        Integer actorRole = actor != null ? actor.getRole() : null;
        boolean isRoot = actorRole != null && actorRole == 0;
        
        // Chỉ ghi đè companyId cho user không phải root
        // User root (role=0) có thể quản lý user từ mọi công ty
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
        permission("setting-member-list");
        return userPermissionRepository.findByUserId(id).stream().map(upe -> {
            MemberPermissionDto dto = new MemberPermissionDto();
            dto.setPermissionId(upe.getPermission().getId());
            dto.setAllowed((int) (upe.getAllowed() != null ? upe.getAllowed() : 0));
            return dto;
        }).collect(Collectors.toList());
    }

    public static class MemberPermissionDto {
        private Long permissionId;
        private Integer allowed;
        public Long getPermissionId() { return permissionId; }
        public void setPermissionId(Long permissionId) { this.permissionId = permissionId; }
        public Integer getAllowed() { return allowed; }
        public void setAllowed(Integer allowed) { this.allowed = allowed; }
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
        UserEntity user = userRepository.findById(id).orElse(null);
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
