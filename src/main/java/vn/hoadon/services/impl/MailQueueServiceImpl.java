package vn.hoadon.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.hoadon.entity.MailJobEntity;
import vn.hoadon.messaging.MailJobMessage;
import vn.hoadon.repositories.MailJobRepository;
import vn.hoadon.services.MailQueueService;

/**
 * Database-backed mail queue — tương tự Laravel Queue driver "database".
 * Job được lưu vào bảng mail_jobs, DbMailQueueWorker poll và xử lý mỗi 5 giây.
 */
@Service
public class MailQueueServiceImpl implements MailQueueService {

    private static final Logger log = LoggerFactory.getLogger(MailQueueServiceImpl.class);

    @Autowired private MailJobRepository mailJobRepository;
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
            if (message.getVariables() != null) {
                job.setSubject(message.getVariables().get("SUBJECT"));
            }
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
}
