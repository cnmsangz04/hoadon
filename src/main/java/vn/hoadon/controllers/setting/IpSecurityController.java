package vn.hoadon.controllers.setting;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.CompanyAllowedIpEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.services.CompanyIpSecurityService;
import vn.hoadon.util.ClientIpUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/setting/security/ip")
public class IpSecurityController extends BaseController {

    private final CompanyIpSecurityService ipSecurityService;

    public IpSecurityController(CompanyIpSecurityService ipSecurityService) {
        this.ipSecurityService = ipSecurityService;
    }

    @GetMapping
    public Map<String, Object> state(HttpServletRequest request) {
        UserEntity actor = requireSettingManager();
        return buildState(actor, request);
    }

    @PostMapping("/status")
    public ResponseEntity<?> status(@RequestBody StatusRequest body, HttpServletRequest request) {
        UserEntity actor = requireSettingManager();
        try {
            ipSecurityService.setEnabled(actor, body != null && Boolean.TRUE.equals(body.getEnabled()), ClientIpUtil.resolve(request));
            return ResponseEntity.ok(buildState(actor, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @PostMapping("/allow")
    public ResponseEntity<?> allow(@RequestBody IpRequest body, HttpServletRequest request) {
        UserEntity actor = requireSettingManager();
        try {
            ipSecurityService.addIp(actor, body != null ? body.getIpAddress() : null, body != null ? body.getNote() : null);
            return ResponseEntity.ok(buildState(actor, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @DeleteMapping("/allow/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        UserEntity actor = requireSettingManager();
        try {
            ipSecurityService.deleteIp(actor, id);
            return ResponseEntity.ok(buildState(actor, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    private Map<String, Object> buildState(UserEntity actor, HttpServletRequest request) {
        Long companyId = actor.getCompanyId();
        Map<String, Object> res = new HashMap<>();
        res.put("companyId", companyId);
        res.put("enabled", ipSecurityService.isSecurityEnabled(companyId));
        res.put("currentIp", ClientIpUtil.resolve(request));
        List<Map<String, Object>> ips = ipSecurityService.listIps(companyId).stream()
                .map(this::toMap)
                .toList();
        res.put("ips", ips);
        return res;
    }

    private Map<String, Object> toMap(CompanyAllowedIpEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entity.getId());
        map.put("companyId", entity.getCompanyId());
        map.put("ipAddress", entity.getIpAddress());
        map.put("originalIp", entity.getOriginalIp());
        map.put("status", entity.getStatus());
        map.put("note", entity.getNote());
        map.put("createdBy", entity.getCreatedBy());
        map.put("createdAt", entity.getCreatedAt());
        map.put("updatedAt", entity.getUpdatedAt());
        return map;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("message", message != null ? message : "Thao tác thất bại");
        return map;
    }

    private UserEntity requireSettingManager() {
        UserEntity actor = currentUser();
        if (actor == null || actor.getRole() == null || actor.getRole() >= 2) {
            throw new AccessDeniedException("Bạn không có quyền cấu hình bảo mật IP");
        }
        if (actor.getCompanyId() == null) {
            throw new AccessDeniedException("Không xác định được công ty hiện tại");
        }
        permission("setting-security-ip");
        return actor;
    }

    public static class StatusRequest {
        private Boolean enabled;
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }

    public static class IpRequest {
        private String ipAddress;
        private String note;
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}
