package vn.hoadon.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.hoadon.entity.MailJobEntity;
import vn.hoadon.entity.MailTemplateEntity;
import vn.hoadon.messaging.MailJobMessage;
import vn.hoadon.repositories.MailJobRepository;
import vn.hoadon.repositories.MailTemplateRepository;
import vn.hoadon.services.MailQueueService;

/**
 * Database-backed mail queue — tương tự Laravel Queue driver "database".
 * Job được lưu vào bảng mail_jobs, DbMailQueueWorker poll và xử lý mỗi 5 giây.
 */
@Service
public class MailQueueServiceImpl implements MailQueueService {

    private static final Logger log = LoggerFactory.getLogger(MailQueueServiceImpl.class);

    @Autowired private MailJobRepository mailJobRepository;
    @Autowired private MailTemplateRepository mailTemplateRepository;
    @Autowired private ObjectMapper objectMapper;

    @Override
    public void enqueue(MailJobMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            long now = System.currentTimeMillis();
            MailJobEntity job = new MailJobEntity();
            job.setPayload(payload);
            job.setInvoiceId(message.getInvoiceId());
            job.setCompanyId(message.getCompanyId());
            job.setTemplateKey(message.getTemplateKey());
            job.setToEmail(message.getToEmail());
            job.setToName(message.getToName());
            job.setSubject(resolveSubject(message));
            job.setShowHistory(shouldShowHistory(message.getTemplateKey()));
            job.setStatus("queued");
            job.setAvailableAt(now);
            job.setCreatedAt(now);
            job.setUpdatedAt(now);
            mailJobRepository.save(job);
            log.info("[MailQueue] Job saved to DB: template={}, to={}", message.getTemplateKey(), message.getToEmail());
        } catch (Exception e) {
            log.error("[MailQueue] Failed to save job: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể đưa email vào hàng đợi", e);
        }
    }

    private String resolveSubject(MailJobMessage message) {
        if (message == null) return null;

        MailTemplateEntity tpl = resolveTemplate(message);
        if (tpl != null && tpl.getTitle() != null && !tpl.getTitle().isBlank()) {
            return tpl.getTitle();
        }

        if (message.getVariables() != null) {
            return message.getVariables().get("SUBJECT");
        }
        return null;
    }

    private boolean shouldShowHistory(String templateKey) {
        return !"LOGIN_INFO_MAIL".equals(templateKey)
                && !"RESET_PASSWORD_MAIL".equals(templateKey);
    }

    private MailTemplateEntity resolveTemplate(MailJobMessage message) {
        if (message.getTemplateKey() == null || message.getTemplateKey().isBlank()) return null;

        if (message.getCompanyId() != null) {
            MailTemplateEntity tpl = mailTemplateRepository.findByKeyAndCompanyId(
                    message.getTemplateKey(), message.getCompanyId().intValue());
            if (tpl != null && tpl.getStatus() != null && tpl.getStatus() == 1) {
                return tpl;
            }
        }

        MailTemplateEntity sysTpl = mailTemplateRepository.findSystemByKey(message.getTemplateKey());
        if (sysTpl != null && sysTpl.getStatus() != null && sysTpl.getStatus() == 1) {
            return sysTpl;
        }
        return null;
    }
}
