package vn.hoadon.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.dto.MemberUpsertRequest;
import vn.hoadon.services.MemberService;
import vn.hoadon.repositories.UserPermissionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/setting/members")
public class MemberController {

    @Autowired
    private MemberService service;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @PostMapping("/list")
    public Page<UserEntity> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Byte status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Integer role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return service.list(keyword, roleId, status, companyId, role, pageable);
    }

    @GetMapping("/list")
    public Page<UserEntity> listGet(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Byte status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Integer role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return service.list(keyword, roleId, status, companyId, role, pageable);
    }

    @PostMapping("/saveOrUpdate")
    public UserEntity saveOrUpdate(@RequestBody MemberUpsertRequest incoming) {
        return service.saveOrUpdate(incoming);
    }

    @GetMapping("/{id}/permissions")
    public List<MemberPermissionDto> getUserPermissions(@PathVariable Long id) {
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
        service.setLock(id, lock == 1);
    }

    @PostMapping("/{id}/reset-password")
    public Map<String, Object> resetPassword(@PathVariable Long id) {
        String tempPassword = service.resetPassword(id);
        Map<String, Object> res = new HashMap<>();
        res.put("temporaryPassword", tempPassword);
        res.put("message", "Đã reset mật khẩu thành công");
        return res;
    }

    @DeleteMapping("/{id}")
    public void removeFromCompany(@PathVariable Long id) {
        // Perform soft-delete for member
        service.delete(id);
    }
}