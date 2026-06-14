package vn.hoadon.worker;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import vn.hoadon.controllers.customers.MailServerController;
import vn.hoadon.entity.MailServerEntity;
import vn.hoadon.entity.MailTemplateEntity;
import vn.hoadon.messaging.MailJobMessage;
import vn.hoadon.repositories.MailServerRepository;
import vn.hoadon.repositories.MailTemplateRepository;

import java.util.Map;
import java.util.Optional;
import java.util.Base64;

/**
 * Processes {@link MailJobMessage} jobs loaded from the SQL Server mail_jobs queue.
 *
 * Sender resolution order:
 *   1. Company-specific mail_servers config (DB, encrypted password)
 *   2. Fallback: application.properties JavaMailSender (if configured)
 *   3. Log warning and skip if neither available
 *
 * Template resolution order:
 *   1. Company-specific template (key + companyId)
 *   2. System template           (key, system=1)
 *   3. Minimal fallback HTML
 *
 * Variable substitution: replaces [KEY] and {{key}} placeholders.
 */
@Component
public class MailWorker {

    private static final Logger log = LoggerFactory.getLogger(MailWorker.class);

    /** Fallback sender from application.properties (optional) */
    @Autowired(required = false)
    private JavaMailSender fallbackMailSender;

    @Autowired private MailTemplateRepository  mailTemplateRepository;
    @Autowired private MailServerRepository    mailServerRepository;
    @Autowired private MailServerController    mailServerController;

    /**
     * Send mail synchronously so DbMailQueueWorker can mark the database job done or failed.
     */
    public void processSync(MailJobMessage job) {
        log.info("[MailWorker] Processing job: template={}, to={}", job.getTemplateKey(), job.getToEmail());
        try {
            Long senderCompanyId = resolveSenderCompanyId(job);
            ResolvedSender rs = resolveSender(senderCompanyId);
            if (rs == null) {
                log.warn("[MailWorker] No mail sender available for companyId={}. Skipping.", senderCompanyId);
                return;
            }

            ResolvedTemplate tpl = resolveTemplate(job);
            String subject = interpolate(tpl.subject, job.getVariables());
            String body    = interpolate(tpl.body,    job.getVariables());

            MailAttachment attachment = resolveAttachment(job);
            sendHtmlMail(rs, job.getToEmail(), job.getToName(), subject, body, attachment);
            log.info("[MailWorker] Mail sent: to={}, subject={}", job.getToEmail(), subject);
        } catch (Exception e) {
            log.error("[MailWorker] Failed: template={}, to={}, error={}",
                    job.getTemplateKey(), job.getToEmail(), e.getMessage(), e);
            throw new RuntimeException(rootMessage(e), e);
        }
    }

    // â”€â”€ Sender resolution â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private ResolvedSender resolveSender(Long companyId) {
        if (companyId != null) {
            Optional<MailServerEntity> opt = mailServerRepository
                    .findFirstByCompanyIdAndStatusOrderByIdDesc(companyId.intValue(), (short) 1);
            if (opt.isPresent()) {
                MailServerEntity cfg = opt.get();
                JavaMailSender sender = mailServerController.buildSender(cfg);
                String from = (cfg.getFromEmail() != null && !cfg.getFromEmail().isBlank())
                        ? cfg.getFromEmail() : cfg.getUsername();
                String name = (cfg.getFromName() != null && !cfg.getFromName().isBlank())
                        ? cfg.getFromName() : from;
                return new ResolvedSender(sender, from, name);
            }
        }
        // Fallback to static config
        if (fallbackMailSender != null) {
            return new ResolvedSender(fallbackMailSender, null, null);
        }
        return null;
    }

    // â”€â”€ Template resolution â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private Long resolveSenderCompanyId(MailJobMessage job) {
        if (job != null && ("LOGIN_INFO_MAIL".equals(job.getTemplateKey())
                || "RESET_PASSWORD_MAIL".equals(job.getTemplateKey()))) {
            return 1L;
        }
        return job != null ? job.getCompanyId() : null;
    }
    private ResolvedTemplate resolveTemplate(MailJobMessage job) {
        String key     = job.getTemplateKey();
        Long companyId = job.getCompanyId();

        if (companyId != null) {
            MailTemplateEntity tpl = mailTemplateRepository.findByKeyAndCompanyId(key, companyId.intValue());
            if (tpl != null && tpl.getStatus() != null && tpl.getStatus() == 1) {
                return new ResolvedTemplate(tpl.getTitle(), tpl.getContent());
            }
        }

        MailTemplateEntity sysTpl = mailTemplateRepository.findSystemByKey(key);
        if (sysTpl != null && sysTpl.getStatus() != null && sysTpl.getStatus() == 1) {
            return new ResolvedTemplate(sysTpl.getTitle(), sysTpl.getContent());
        }

        if ("LOGIN_INFO_MAIL".equals(key) || "RESET_PASSWORD_MAIL".equals(key)) {
            throw new IllegalStateException("Chưa cấu hình template email LOGIN_INFO_MAIL đang hoạt động trong mail_templates");
        }

        if (job.getVariables() != null) {
            String subject = job.getVariables().get("SUBJECT");
            String htmlBody = job.getVariables().get("HTML_BODY");
            if (subject != null && !subject.isBlank() && htmlBody != null && !htmlBody.isBlank()) {
                return new ResolvedTemplate(subject, htmlBody);
            }
        }

        log.warn("[MailWorker] No active template for key='{}', companyId={}", key, companyId);
        StringBuilder fb = new StringBuilder("<div style='font-family:Arial,sans-serif'>");
        fb.append("<p>Xin chÃ o ").append(esc(job.getToName())).append(",</p>");
        if (job.getVariables() != null) {
            fb.append("<ul>");
            for (Map.Entry<String, String> e : job.getVariables().entrySet()) {
                fb.append("<li>").append(esc(e.getKey())).append(": ").append(esc(e.getValue())).append("</li>");
            }
            fb.append("</ul>");
        }
        fb.append("</div>");
        return new ResolvedTemplate("Thông báo từ hệ thống hóa đơn", fb.toString());
    }

    // â”€â”€ Variable substitution â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private String interpolate(String template, Map<String, String> vars) {
        if (template == null) return "";
        if (vars == null || vars.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace("[" + entry.getKey() + "]", value)
                    .replace("{{" + entry.getKey() + "}}", value);
        }
        return result;
    }

    // â”€â”€ Mail sending â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private MailAttachment resolveAttachment(MailJobMessage job) {
        if (job == null || job.getVariables() == null) return null;
        String name = job.getVariables().get("ATTACHMENT_ZIP_NAME");
        String base64 = job.getVariables().get("ATTACHMENT_ZIP_BASE64");
        if (name == null || name.isBlank() || base64 == null || base64.isBlank()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            if (bytes.length == 0) return null;
            return new MailAttachment(name, bytes, "application/zip");
        } catch (Exception e) {
            throw new IllegalArgumentException("File ZIP đính kèm không hợp lệ", e);
        }
    }

    private void sendHtmlMail(ResolvedSender rs, String to, String toName,
                               String subject, String htmlBody, MailAttachment attachment) throws Exception {
        MimeMessage message = rs.sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        if (rs.fromEmail != null) {
            helper.setFrom(new InternetAddress(rs.fromEmail, rs.fromName != null ? rs.fromName : rs.fromEmail, "UTF-8"));
        }
        if (toName != null && !toName.isBlank()) {
            helper.setTo(new InternetAddress(to, toName, "UTF-8"));
        } else {
            helper.setTo(to);
        }
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        if (attachment != null) {
            helper.addAttachment(attachment.name(), new ByteArrayResource(attachment.bytes()), attachment.contentType());
        }
        rs.sender.send(message);
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getMessage();
        }
        return message != null && !message.isBlank() ? message : "Gửi mail thất bại";
    }

    private record ResolvedTemplate(String subject, String body) {}
    private record ResolvedSender(JavaMailSender sender, String fromEmail, String fromName) {}
    private record MailAttachment(String name, byte[] bytes, String contentType) {}
}
