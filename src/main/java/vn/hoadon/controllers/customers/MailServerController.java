package vn.hoadon.controllers.customers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.MailServerEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.MailServerRepository;
import vn.hoadon.util.AesEncryptor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/mail-servers")
public class MailServerController extends BaseController {

    @Autowired private MailServerRepository mailServerRepository;
    @Autowired private AesEncryptor aesEncryptor;

    /** GET /v1/mail-servers — lấy cấu hình máy chủ gửi mail của công ty */
    @GetMapping
    public ResponseEntity<?> get(@AuthenticationPrincipal UserEntity user) {
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }
        permission("mail-server-manage");
        Optional<MailServerEntity> opt = mailServerRepository
                .findFirstByCompanyIdAndStatusOrderByIdDesc(user.getCompanyId().intValue(), (short) 1);

        if (opt.isEmpty()) {
            // Trả về cấu hình mặc định rỗng
            return ResponseEntity.ok(defaultConfig());
        }
        return ResponseEntity.ok(toResponse(opt.get()));
    }

    /** POST /v1/mail-servers — lưu / cập nhật cấu hình (tạo mới nếu chưa có) */
    @PostMapping
    public ResponseEntity<?> save(@AuthenticationPrincipal UserEntity user,
                                  @RequestBody MailServerRequest req) {
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }
        permission("mail-server-manage");

        int companyId = user.getCompanyId().intValue();

        // Tìm bản ghi cũ hoặc tạo mới
        MailServerEntity entity = mailServerRepository
                .findFirstByCompanyIdAndStatusOrderByIdDesc(companyId, (short) 1)
                .orElse(new MailServerEntity());

        boolean isNew = entity.getId() == null;

        entity.setCompanyId(companyId);
        entity.setHost(req.host != null ? req.host.trim() : "smtp.gmail.com");
        entity.setPort(req.port != null ? req.port : 587);
        entity.setUsername(req.username != null ? req.username.trim() : "");
        entity.setFromName(req.fromName != null ? req.fromName.trim() : "");
        entity.setFromEmail(req.fromEmail != null ? req.fromEmail.trim() : "");
        entity.setEncryption(req.encryption != null ? req.encryption : (short) 1);
        entity.setStatus((short) 1);

        // Mã hóa password — nếu frontend gửi chuỗi rỗng hoặc "••••" thì giữ nguyên password cũ
        String rawPw = req.password;
        if (rawPw != null && !rawPw.isBlank() && !rawPw.startsWith("•")) {
            entity.setPassword(aesEncryptor.encrypt(rawPw));
        } else if (isNew) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập mật khẩu"));
        }
        // else: giữ nguyên password cũ khi cập nhật

        if (isNew) entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        mailServerRepository.save(entity);

        return ResponseEntity.ok(Map.of("message", "Đã lưu cấu hình máy chủ gửi mail"));
    }

    /** POST /v1/mail-servers/test — gửi mail test */
    @PostMapping("/test")
    public ResponseEntity<?> test(@AuthenticationPrincipal UserEntity user,
                                  @RequestBody Map<String, Object> body) {
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }
        permission("mail-server-manage");

        Optional<MailServerEntity> opt = mailServerRepository
                .findFirstByCompanyIdAndStatusOrderByIdDesc(user.getCompanyId().intValue(), (short) 1);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chưa cấu hình máy chủ gửi mail"));
        }

        String toEmail = body.get("email") != null ? body.get("email").toString().trim() : null;
        if (toEmail == null || toEmail.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập địa chỉ email kiểm tra"));
        }

        try {
            MailServerEntity cfg = opt.get();
            org.springframework.mail.javamail.JavaMailSender sender = buildSender(cfg);

            jakarta.mail.internet.MimeMessage message = sender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper =
                    new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");

            String fromAddr = (cfg.getFromEmail() != null && !cfg.getFromEmail().isBlank())
                    ? cfg.getFromEmail() : cfg.getUsername();
            String fromName = (cfg.getFromName() != null && !cfg.getFromName().isBlank())
                    ? cfg.getFromName() : fromAddr;

            helper.setFrom(new jakarta.mail.internet.InternetAddress(fromAddr, fromName, "UTF-8"));
            helper.setTo(toEmail);
            helper.setSubject("Kiểm tra cấu hình máy chủ gửi mail");
            helper.setText("<p>Email kiểm tra từ hệ thống hóa đơn điện tử.<br>Cấu hình đang hoạt động bình thường.</p>", true);
            sender.send(message);

            return ResponseEntity.ok(Map.of("message", "Gửi mail kiểm tra thành công đến " + toEmail));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Gửi thất bại: " + e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public org.springframework.mail.javamail.JavaMailSender buildSender(MailServerEntity cfg) {
        org.springframework.mail.javamail.JavaMailSenderImpl sender =
                new org.springframework.mail.javamail.JavaMailSenderImpl();
        sender.setHost(cfg.getHost());
        sender.setPort(cfg.getPort());
        sender.setUsername(cfg.getUsername());
        String password = aesEncryptor.decrypt(cfg.getPassword());
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("SMTP password trống hoặc không hợp lệ");
        }
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");

        java.util.Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        Short enc = cfg.getEncryption();
        if (enc == null || enc == 1) {
            // STARTTLS
            props.put("mail.smtp.starttls.enable",  "true");
            props.put("mail.smtp.starttls.required", "true");
        } else if (enc == 2) {
            // SSL/TLS
            props.put("mail.smtp.ssl.enable", "true");
        }
        return sender;
    }

    private Map<String, Object> toResponse(MailServerEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         e.getId());
        m.put("host",       e.getHost());
        m.put("port",       e.getPort());
        m.put("username",   e.getUsername());
        m.put("password",   "••••••••"); // never expose encrypted value
        m.put("fromName",   e.getFromName());
        m.put("fromEmail",  e.getFromEmail());
        m.put("encryption", e.getEncryption());
        m.put("status",     e.getStatus());
        return m;
    }

    private Map<String, Object> defaultConfig() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         null);
        m.put("host",       "smtp.gmail.com");
        m.put("port",       587);
        m.put("username",   "");
        m.put("password",   "");
        m.put("fromName",   "");
        m.put("fromEmail",  "");
        m.put("encryption", 1);
        m.put("status",     1);
        return m;
    }

    // ── Request DTO ───────────────────────────────────────────────────────────

    public static class MailServerRequest {
        public String  host;
        public Integer port;
        public String  username;
        public String  password;
        public String  fromName;
        public String  fromEmail;
        public Short   encryption;
    }
}
