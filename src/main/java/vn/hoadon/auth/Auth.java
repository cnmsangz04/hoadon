package vn.hoadon.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import vn.hoadon.dto.auth.AuthRequest;
import vn.hoadon.dto.auth.AuthResponse;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.CompanyRegistrationRequestEntity;
import vn.hoadon.entity.LoginSessionEntity;
import vn.hoadon.entity.LoginHistoryEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.messaging.MailJobMessage;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.CompanyRegistrationRequestRepository;
import vn.hoadon.repositories.LoginHistoryRepository;
import vn.hoadon.repositories.LoginSessionRepository;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.security.JwtUtil;
import vn.hoadon.services.CompanyIpSecurityService;
import vn.hoadon.services.MailQueueService;
import vn.hoadon.services.UserService;
import vn.hoadon.util.ClientIpUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/auth")
public class Auth {

    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private CompanyRegistrationRequestRepository companyRegistrationRequestRepository;
    @Autowired private MailQueueService mailQueueService;
    @Autowired private LoginHistoryRepository loginHistoryRepository;
    @Autowired private LoginSessionRepository loginSessionRepository;
    @Autowired private CompanyIpSecurityService companyIpSecurityService;

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req, HttpServletRequest httpReq) {
        Optional<UserEntity> uOpt = Optional.empty();
        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            uOpt = userService.findByUsername(req.getUsername().trim());
        }
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        UserEntity user = uOpt.get();
        if (user.getStatus() == null || user.getStatus() == 0) {
            return ResponseEntity.status(403).body("Account inactive");
        }

        boolean passwordMatch = passwordEncoder.matches(req.getPassword(), user.getPassword());
        if (!passwordMatch) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String ipAddress = ClientIpUtil.resolve(httpReq);
        if (!companyIpSecurityService.isAllowed(user, ipAddress)) {
            return ResponseEntity.status(403).body("IP đăng nhập chưa được phép truy cập");
        }

        String sessionId = UUID.randomUUID().toString().replaceAll("-", "");
        LoginSessionEntity session = createSession(user, httpReq, ipAddress, "USER", sessionId);
        String token = jwtUtil.generateToken(user, sessionId);
        session.setExpiresAt(jwtUtil.getExpiration(token));
        loginSessionRepository.save(session);
        recordLogin(user, httpReq, ipAddress, "USER");
        return ResponseEntity.ok(new AuthResponse(token, "Bearer", user.getId(), user.getRole()));
    }

    @PostMapping("/login-admin")
    public ResponseEntity<?> loginAdmin(@RequestBody AuthRequest req, HttpServletRequest httpReq) {
        Optional<UserEntity> uOpt = Optional.empty();
        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            uOpt = userService.findByUsername(req.getUsername().trim());
        }
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        UserEntity user = uOpt.get();
        if (user.getStatus() == null || user.getStatus() == 0) {
            return ResponseEntity.status(403).body("Account inactive");
        }

        Integer role = user.getRole();
        boolean isRoot = role != null && role == 0;
        boolean isSystemAdmin = role != null && role == 1;
        if (!(isRoot || isSystemAdmin)) {
            return ResponseEntity.status(403).body("Not an admin account");
        }

        if (user.getAdminPassword() == null || user.getAdminPassword().isEmpty()) {
            return ResponseEntity.status(403).body("No admin password set");
        }

        boolean adminPasswordMatch = passwordEncoder.matches(req.getPassword(), user.getAdminPassword());
        if (!adminPasswordMatch) {
            return ResponseEntity.status(401).body("Invalid admin credentials");
        }

        String ipAddress = ClientIpUtil.resolve(httpReq);
        if (!companyIpSecurityService.isAllowed(user, ipAddress)) {
            return ResponseEntity.status(403).body("IP đăng nhập chưa được phép truy cập");
        }

        String sessionId = UUID.randomUUID().toString().replaceAll("-", "");
        LoginSessionEntity session = createSession(user, httpReq, ipAddress, "ADMIN", sessionId);
        String token = jwtUtil.generateToken(user, sessionId);
        session.setExpiresAt(jwtUtil.getExpiration(token));
        loginSessionRepository.save(session);
        recordLogin(user, httpReq, ipAddress, "ADMIN");
        return ResponseEntity.ok(new AuthResponse(token, "Bearer", user.getId(), user.getRole()));
    }

    @PostMapping("/forgot")
    public ResponseEntity<?> forgot(@RequestBody ForgotRequest req, HttpServletRequest httpReq) {
        if (req == null || req.getUsername() == null || req.getEmail() == null) {
            return ResponseEntity.badRequest().body("Thiếu thông tin yêu cầu");
        }

        String username = req.getUsername().trim();
        String email = req.getEmail().trim();
        Optional<UserEntity> uOpt = userRepository.findByUsername(username);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Tài khoản không tồn tại");
        }

        UserEntity user = uOpt.get();
        String userEmail = user.getEmail() != null ? user.getEmail().trim() : null;
        if (userEmail == null || !userEmail.equalsIgnoreCase(email)) {
            return ResponseEntity.status(400).body("Email không khớp với email của tài khoản");
        }

        String token = UUID.randomUUID().toString().replaceAll("-", "");
        user.setForgetToken(token);
        user.setTimeReset(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        String resetLink = buildResetLink(httpReq, token);
        boolean mailOk = enqueueResetPasswordMail(user, resetLink);
        return ResponseEntity.ok(new ForgotResponse(token, mailOk));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (req == null || isBlank(req.getCompanyName()) || isBlank(req.getTaxcode())
                || isBlank(req.getAddress()) || isBlank(req.getEmail())) {
            return ResponseEntity.badRequest().body("Vui l\u00f2ng nh\u1eadp \u0111\u1ea7y \u0111\u1ee7 th\u00f4ng tin \u0111\u0103ng k\u00fd");
        }

        String email = req.getEmail().trim();
        String taxcode = req.getTaxcode().trim();

        if (companyRepository.existsByTaxcode(taxcode)) {
            return ResponseEntity.status(400).body("M\u00e3 s\u1ed1 thu\u1ebf \u0111\u00e3 \u0111\u01b0\u1ee3c s\u1eed d\u1ee5ng b\u1edfi c\u00f4ng ty kh\u00e1c");
        }
        if (companyRegistrationRequestRepository.existsByTaxcodeAndStatus(taxcode, 0)) {
            return ResponseEntity.status(400).body("H\u1ed3 s\u01a1 \u0111\u0103ng k\u00fd v\u1edbi m\u00e3 s\u1ed1 thu\u1ebf n\u00e0y \u0111ang ch\u1edd duy\u1ec7t");
        }

        CompanyRegistrationRequestEntity request = new CompanyRegistrationRequestEntity();
        request.setCompanyName(req.getCompanyName().trim());
        request.setTaxcode(taxcode);
        request.setAddress(req.getAddress().trim());
        request.setEmail(email);
        request.setPhone(trimToNull(req.getPhone()));
        request.setContactName(trimToNull(req.getContactName()));
        request.setStatus(0);
        request = companyRegistrationRequestRepository.save(request);

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "\u0110\u0103ng k\u00fd th\u00e0nh c\u00f4ng. H\u1ed3 s\u01a1 \u0111ang ch\u1edd qu\u1ea3n tr\u1ecb vi\u00ean duy\u1ec7t.");
        resp.put("requestId", request.getId());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/forgot/resend")
    public ResponseEntity<?> resend(@RequestBody ResendRequest req, HttpServletRequest httpReq) {
        final String initialToken = req != null ? req.getRequestId() : null;
        String token = initialToken;
        UserEntity user = null;

        if (initialToken != null && !initialToken.isBlank()) {
            user = userRepository.findAll().stream()
                    .filter(u -> initialToken.equals(u.getForgetToken()))
                    .findFirst()
                    .orElse(null);
        }

        if (user == null) {
            if (req == null || req.getUsername() == null || req.getEmail() == null) {
                return ResponseEntity.badRequest().body("Thiếu thông tin yêu cầu");
            }

            Optional<UserEntity> uOpt = userRepository.findByUsername(req.getUsername().trim());
            if (uOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Tài khoản không tồn tại");
            }

            user = uOpt.get();
            String userEmail = user.getEmail() != null ? user.getEmail().trim() : null;
            if (userEmail == null || !userEmail.equalsIgnoreCase(req.getEmail().trim())) {
                return ResponseEntity.status(400).body("Email không khớp với email của tài khoản");
            }

            if (user.getForgetToken() == null || user.getTimeReset() == null
                    || user.getTimeReset().isBefore(LocalDateTime.now())) {
                token = UUID.randomUUID().toString().replaceAll("-", "");
                user.setForgetToken(token);
                user.setTimeReset(LocalDateTime.now().plusMinutes(5));
                userRepository.save(user);
            } else {
                token = user.getForgetToken();
            }
        }

        String resetLink = buildResetLink(httpReq, token);
        boolean mailOk = enqueueResetPasswordMail(user, resetLink);
        return ResponseEntity.ok(new ForgotResponse(token, mailOk));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        if (req == null || req.getToken() == null || req.getPassword() == null) {
            return ResponseEntity.badRequest().body("Thiếu thông tin yêu cầu");
        }

        String token = req.getToken().trim();
        UserEntity user = userRepository.findAll().stream()
                .filter(u -> token.equals(u.getForgetToken()))
                .findFirst()
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body("Token không hợp lệ");
        }
        if (user.getTimeReset() == null || user.getTimeReset().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body("Token đã hết hạn");
        }

        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setForgetToken(null);
        user.setTimeReset(null);
        userRepository.save(user);
        return ResponseEntity.ok("Cập nhật mật khẩu thành công");
    }

    private String buildResetLink(HttpServletRequest httpReq, String token) {
        String baseUrl = frontendUrl;
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = ServletUriComponentsBuilder.fromRequestUri(httpReq)
                    .replacePath(null)
                    .build()
                    .toUriString();
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        }
        return baseUrl.replaceAll("/+$", "") + "/auth/reset-password/" + token;
    }

    private boolean enqueueResetPasswordMail(UserEntity user, String resetLink) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return false;
        }

        try {
            CompanyEntity company = user.getCompanyId() != null
                    ? companyRepository.findById(user.getCompanyId()).orElse(null)
                    : null;
            String name = user.getName() != null && !user.getName().isBlank()
                    ? user.getName()
                    : user.getUsername();

            Map<String, String> vars = new HashMap<>();
            vars.put("SUBJECT", "Đặt lại mật khẩu");
            vars.put("NAME", name != null ? name : "");
            vars.put("USERNAME", user.getUsername() != null ? user.getUsername() : "");
            vars.put("EMAIL", user.getEmail());
            vars.put("LINK", resetLink != null ? resetLink : "");
            vars.put("COMPANY", company != null && company.getName() != null ? company.getName() : "");
            vars.put("COM_NAME", vars.get("COMPANY"));

            MailJobMessage msg = new MailJobMessage();
            msg.setTemplateKey("RESET_PASSWORD_MAIL");
            msg.setCompanyId(user.getCompanyId());
            msg.setToEmail(user.getEmail().trim());
            msg.setToName(name);
            msg.setVariables(vars);
            mailQueueService.enqueue(msg);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String generateCompanyPrefix() {
        String prefix;
        do {
            prefix = "CP" + System.currentTimeMillis();
        } while (companyRepository.existsByPrefix(prefix));
        return prefix;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void recordLogin(UserEntity user, HttpServletRequest request, String ipAddress, String loginType) {
        try {
            LoginHistoryEntity entity = new LoginHistoryEntity();
            entity.setCompanyId(user.getCompanyId());
            entity.setUserId(user.getId());
            entity.setUsername(user.getUsername());
            entity.setIpAddress(ipAddress);
            entity.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
            entity.setLoginType(loginType);
            entity.setLoginAt(LocalDateTime.now());
            loginHistoryRepository.save(entity);
        } catch (Exception ignored) {
            // Không để lỗi ghi lịch sử làm gián đoạn đăng nhập.
        }
    }

    private LoginSessionEntity createSession(
            UserEntity user,
            HttpServletRequest request,
            String ipAddress,
            String loginType,
            String sessionId) {
        LoginSessionEntity session = new LoginSessionEntity();
        session.setSessionId(sessionId);
        session.setUserId(user.getId());
        session.setCompanyId(user.getCompanyId());
        session.setUsername(user.getUsername());
        session.setLoginType(loginType);
        session.setIpAddress(ipAddress);
        session.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
        session.setIssuedAt(LocalDateTime.now());
        session.setLastSeenAt(LocalDateTime.now());
        return loginSessionRepository.save(session);
    }

    public static class ForgotRequest {
        private String username;
        private String email;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResendRequest {
        private String requestId;
        private String username;
        private String email;
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResetPasswordRequest {
        private String token;
        private String password;
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        private String companyName;
        private String taxcode;
        private String address;
        private String email;
        private String phone;
        private String contactName;
        private String username;
        private String password;

        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getTaxcode() { return taxcode; }
        public void setTaxcode(String taxcode) { this.taxcode = taxcode; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getContactName() { return contactName; }
        public void setContactName(String contactName) { this.contactName = contactName; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class ForgotResponse {
        private String requestId;
        private boolean mailSent;
        public ForgotResponse(String requestId, boolean mailSent) {
            this.requestId = requestId;
            this.mailSent = mailSent;
        }
        public String getRequestId() { return requestId; }
        public boolean isMailSent() { return mailSent; }
    }
}
