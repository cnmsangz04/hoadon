package vn.hoadon.controllers.setting;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.LoginSessionEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.LoginSessionRepository;
import vn.hoadon.security.JwtUtil;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/setting/sessions")
public class SessionController extends BaseController {

    private final LoginSessionRepository loginSessionRepository;
    private final JwtUtil jwtUtil;

    public SessionController(LoginSessionRepository loginSessionRepository, JwtUtil jwtUtil) {
        this.loginSessionRepository = loginSessionRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request) {
        UserEntity user = currentUser();
        if (user == null || user.getId() == null) {
            return ResponseEntity.status(403).build();
        }
        String currentSid = currentSessionId(request);
        String currentLoginType = currentLoginType(currentSid);
        List<LoginSessionEntity> sessions = currentLoginType != null
                ? loginSessionRepository.findByUserIdAndLoginTypeOrderByLastSeenAtDesc(user.getId(), currentLoginType)
                : loginSessionRepository.findByUserIdOrderByLastSeenAtDesc(user.getId());
        List<Map<String, Object>> rows = sessions
                .stream()
                .map(session -> toRow(session, currentSid))
                .toList();
        return ResponseEntity.ok(Map.of("items", rows));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> revoke(@PathVariable String sessionId, HttpServletRequest request) {
        UserEntity user = currentUser();
        if (user == null || user.getId() == null) {
            return ResponseEntity.status(403).build();
        }
        LoginSessionEntity session = loginSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session == null || !user.getId().equals(session.getUserId())) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy phiên đăng nhập"));
        }
        String currentLoginType = currentLoginType(currentSessionId(request));
        if (currentLoginType != null && !currentLoginType.equals(session.getLoginType())) {
            return ResponseEntity.status(403).body(Map.of("message", "Không thể thao tác phiên đăng nhập khác loại"));
        }
        revokeSession(session, "USER_REVOKED");
        boolean current = sessionId.equals(currentSessionId(request));
        return ResponseEntity.ok(Map.of("message", current ? "Đã đăng xuất phiên hiện tại" : "Đã đăng xuất phiên", "current", current));
    }

    @PostMapping("/logout-others")
    public ResponseEntity<?> logoutOthers(HttpServletRequest request) {
        UserEntity user = currentUser();
        if (user == null || user.getId() == null) {
            return ResponseEntity.status(403).build();
        }
        String currentSid = currentSessionId(request);
        String currentLoginType = currentLoginType(currentSid);
        List<LoginSessionEntity> sessions = currentLoginType != null
                ? loginSessionRepository.findByUserIdAndLoginTypeOrderByLastSeenAtDesc(user.getId(), currentLoginType)
                : loginSessionRepository.findByUserIdOrderByLastSeenAtDesc(user.getId());
        int count = 0;
        for (LoginSessionEntity session : sessions) {
            if (session.getRevokedAt() != null) continue;
            if (currentSid != null && currentSid.equals(session.getSessionId())) continue;
            revokeSession(session, "LOGOUT_OTHERS");
            count++;
        }
        return ResponseEntity.ok(Map.of("message", "Đã đăng xuất các thiết bị khác", "count", count));
    }

    private void revokeSession(LoginSessionEntity session, String reason) {
        session.setRevokedAt(LocalDateTime.now());
        session.setRevokedReason(reason);
        loginSessionRepository.save(session);
    }

    private Map<String, Object> toRow(LoginSessionEntity session, String currentSid) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", session.getId());
        row.put("sessionId", session.getSessionId());
        row.put("username", session.getUsername());
        row.put("loginType", session.getLoginType());
        row.put("ipAddress", session.getIpAddress());
        row.put("userAgent", session.getUserAgent());
        row.put("issuedAt", session.getIssuedAt());
        row.put("expiresAt", session.getExpiresAt());
        row.put("lastSeenAt", session.getLastSeenAt());
        row.put("revokedAt", session.getRevokedAt());
        row.put("revokedReason", session.getRevokedReason());
        row.put("current", currentSid != null && currentSid.equals(session.getSessionId()));
        row.put("active", session.getRevokedAt() == null && (session.getExpiresAt() == null || session.getExpiresAt().isAfter(LocalDateTime.now())));
        return row;
    }

    private String currentSessionId(HttpServletRequest request) {
        try {
            String header = request != null ? request.getHeader("Authorization") : null;
            if (header == null || !header.startsWith("Bearer ")) return null;
            return jwtUtil.getSessionId(header.substring(7));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String currentLoginType(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return null;
        return loginSessionRepository.findBySessionId(sessionId)
                .map(LoginSessionEntity::getLoginType)
                .filter(type -> type != null && !type.isBlank())
                .orElse(null);
    }
}
