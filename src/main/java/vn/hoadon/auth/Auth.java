package vn.hoadon.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import vn.hoadon.services.UserService;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.dto.auth.AuthRequest;
import vn.hoadon.dto.auth.AuthResponse;
import vn.hoadon.security.JwtUtil;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.entity.CompanyEntity;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/v1/auth")
public class Auth {

    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired(required = false) private JavaMailSender mailSender;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {

        // Login with username only
        Optional<UserEntity> uOpt = Optional.empty();
        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            uOpt = userService.findByUsername(req.getUsername().trim());
        }
        if (uOpt.isEmpty())
            return ResponseEntity.status(401).body("Invalid credentials");

        UserEntity user = uOpt.get();
        if (user.getStatus() == null || user.getStatus() == 0)
            return ResponseEntity.status(403).body("Account inactive");

        boolean passwordMatch = passwordEncoder.matches(req.getPassword(), user.getPassword());
        if (!passwordMatch)
            return ResponseEntity.status(401).body("Invalid credentials");

        String token = jwtUtil.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, "Bearer", user.getId(), user.getRole()));
    }

    @PostMapping("/login-admin")
    public ResponseEntity<?> loginAdmin(@RequestBody AuthRequest req) {

        // Login with username only
        Optional<UserEntity> uOpt = Optional.empty();
        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            uOpt = userService.findByUsername(req.getUsername().trim());
        }
        if (uOpt.isEmpty())
            return ResponseEntity.status(401).body("Invalid credentials");

        UserEntity user = uOpt.get();
        if (user.getStatus() == null || user.getStatus() == 0)
            return ResponseEntity.status(403).body("Account inactive");

        Integer role = user.getRole();
        boolean isRoot = role != null && role == 0;
        boolean isSystemAdmin = role != null && role == 1;

        // Must be admin account
        if (!(isRoot || isSystemAdmin))
            return ResponseEntity.status(403).body("Not an admin account");

        // Always require and validate admin password for admin login
        if (user.getAdminPassword() == null || user.getAdminPassword().isEmpty())
            return ResponseEntity.status(403).body("No admin password set");

        boolean adminPasswordMatch = passwordEncoder.matches(req.getPassword(), user.getAdminPassword());
        if (!adminPasswordMatch)
            return ResponseEntity.status(401).body("Invalid admin credentials");

        String token = jwtUtil.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, "Bearer", user.getId(), user.getRole()));
    }

    // Forgot password: verify username and company email, issue token valid for 5 minutes, send email
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
        // generate token (32 chars) and expiry 5 minutes
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        user.setForgetToken(token);
        user.setTimeReset(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);
        // dynamic base url
        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(httpReq)
                .replacePath(null).build().toUriString();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        }
        String resetLink = baseUrl + "/auth/reset-password/" + token;
        boolean mailOk = false;
        if (mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(userEmail);
                msg.setSubject("Liên kết đặt lại mật khẩu");
                msg.setText("Liên kết đặt lại mật khẩu: " + resetLink);
                mailSender.send(msg);
                mailOk = true;
            } catch (Exception ex) {}
        }
        return ResponseEntity.ok(new ForgotResponse(token, mailOk));
    }

    // Resend email: by requestId/token or by username+email
    @PostMapping("/forgot/resend")
    public ResponseEntity<?> resend(@RequestBody ResendRequest req, HttpServletRequest httpReq) {
        final String initialToken = (req != null ? req.getRequestId() : null);
        String token = initialToken;
        UserEntity user = null;
        if (initialToken != null && !initialToken.isBlank()) {
            // find by existing token using effectively-final variable
            user = userRepository.findAll().stream()
                    .filter(u -> initialToken.equals(u.getForgetToken()))
                    .findFirst().orElse(null);
        }
        if (user == null) {
            if (req == null || req.getUsername() == null || req.getEmail() == null) {
                return ResponseEntity.badRequest().body("Thiếu thông tin yêu cầu");
            }
            Optional<UserEntity> uOpt = userRepository.findByUsername(req.getUsername().trim());
            if (uOpt.isEmpty()) return ResponseEntity.status(404).body("Tài khoản không tồn tại");
            user = uOpt.get();
            String userEmail = user.getEmail() != null ? user.getEmail().trim() : null;
            if (userEmail == null || !userEmail.equalsIgnoreCase(req.getEmail().trim())) {
                return ResponseEntity.status(400).body("Email không khớp với email của tài khoản");
            }
            // reuse or regenerate token
            if (user.getForgetToken() == null || user.getTimeReset() == null || user.getTimeReset().isBefore(LocalDateTime.now())) {
                String newToken = UUID.randomUUID().toString().replaceAll("-", "");
                user.setForgetToken(newToken);
                user.setTimeReset(LocalDateTime.now().plusMinutes(5));
                userRepository.save(user);
                token = newToken;
            } else {
                token = user.getForgetToken();
            }
        }
        final String effectiveToken = token;
        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(httpReq).replacePath(null).build().toUriString();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        }
        String resetLink = baseUrl + "/auth/reset-password/" + effectiveToken;
        boolean mailOk = false;
        if (mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                String userEmail = user.getEmail();
                msg.setTo(userEmail);
                msg.setSubject("Liên kết đặt lại mật khẩu");
                msg.setText("Liên kết đặt lại mật khẩu: " + resetLink);
                mailSender.send(msg);
                mailOk = true;
            } catch (Exception ex) {}
        }
        return ResponseEntity.ok(new ForgotResponse(effectiveToken, mailOk));
    }

    // Reset password using token
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        if (req == null || req.getToken() == null || req.getPassword() == null) {
            return ResponseEntity.badRequest().body("Thiếu thông tin yêu cầu");
        }
        String token = req.getToken().trim();
        UserEntity user = userRepository.findAll().stream()
                .filter(u -> token.equals(u.getForgetToken()))
                .findFirst().orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body("Token không hợp lệ");
        }
        if (user.getTimeReset() == null || user.getTimeReset().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body("Token đã hết hạn");
        }
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        // clear token after reset
        user.setForgetToken(null);
        user.setTimeReset(null);
        userRepository.save(user);
        return ResponseEntity.ok("Cập nhật mật khẩu thành công");
    }

    // DTOs for requests/responses
    public static class ForgotRequest {
        private String username;
        private String email;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResendRequest {
        private String requestId; // reuse token
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

    public static class ForgotResponse {
        private String requestId; // token
        private boolean mailSent;
        public ForgotResponse(String requestId, boolean mailSent) {
            this.requestId = requestId; this.mailSent = mailSent;
        }
        public String getRequestId() { return requestId; }
        public boolean isMailSent() { return mailSent; }
    }
}