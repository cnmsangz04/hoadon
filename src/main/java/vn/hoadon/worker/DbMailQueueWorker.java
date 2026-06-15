package vn.hoadon.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.hoadon.entity.MailJobEntity;
import vn.hoadon.messaging.MailJobMessage;
import vn.hoadon.repositories.MailJobRepository;

import java.util.List;

/**
 * Bộ xử lý hàng đợi mail trong cơ sở dữ liệu.
 *
 * Mỗi 5 giây quét bảng mail_jobs, lấy job đang chờ và xử lý:
 *   - Thành công: đánh dấu đã gửi.
 *   - Thất bại: thử lại tối đa maxAttempts lần, sau đó đánh dấu failed = true.
 *   - Khoảng cách thử lại: 60 giây sau mỗi lần thất bại.
 */
@Component
public class DbMailQueueWorker {

    private static final Logger log = LoggerFactory.getLogger(DbMailQueueWorker.class);
    private static final int BATCH_SIZE = 5;

    @Autowired private MailJobRepository mailJobRepository;
    @Autowired private MailWorker mailWorker;
    @Autowired private ObjectMapper objectMapper;

    @Value("${mail.queue.max-retries:3}")
    private int maxAttempts;

    /**
     * Quét hàng đợi mỗi 5 giây. fixedDelay chỉ bắt đầu đếm lại sau khi xử lý xong,
     * giúp tránh chồng chéo nếu một lượt xử lý chạy lâu.
     */
    @Scheduled(fixedDelay = 5000)
    public void processQueue() {
        long now = System.currentTimeMillis();
        List<MailJobEntity> jobs = mailJobRepository
                .findAvailableJobs(now, PageRequest.of(0, BATCH_SIZE));

        if (jobs.isEmpty()) return;

        log.debug("[MailQueue] Found {} pending job(s)", jobs.size());
        for (MailJobEntity job : jobs) {
            processJob(job);
        }
    }

    private void processJob(MailJobEntity job) {
        // 1. Đánh dấu reserved để worker khác không lấy trùng job khi chạy nhiều instance.
        long now = System.currentTimeMillis();
        job.setReservedAt(now);
        job.setLastAttemptAt(now);
        job.setUpdatedAt(now);
        job.setStatus("processing");
        job.setAttempts(job.getAttempts() + 1);
        mailJobRepository.save(job);

        try {
            // 2. Đọc dữ liệu JSON của job.
            MailJobMessage msg = objectMapper.readValue(job.getPayload(), MailJobMessage.class);

            // 3. Gửi mail đồng bộ trong thread scheduler.
            mailWorker.processSync(msg);

            // 4. Gửi thành công thì lưu trạng thái đã gửi.
            long doneAt = System.currentTimeMillis();
            job.setStatus("sent");
            job.setSentAt(doneAt);
            job.setUpdatedAt(doneAt);
            job.setReservedAt(null);
            job.setError(null);
            mailJobRepository.save(job);
            log.info("[MailQueue] Job {} done (template={}, to={})",
                    job.getId(), msg.getTemplateKey(), msg.getToEmail());

        } catch (Exception e) {
            String errorMessage = rootMessage(e);
            log.error("[MailQueue] Job {} failed (attempt {}/{}): {}",
                    job.getId(), job.getAttempts(), maxAttempts, errorMessage);

            if (job.getAttempts() >= maxAttempts) {
                // Thất bại vĩnh viễn, không retry nữa.
                job.setFailed(true);
                job.setStatus("failed");
                job.setError(truncate(errorMessage, 2000));
                job.setReservedAt(null);
                job.setFailedAt(System.currentTimeMillis());
                job.setUpdatedAt(System.currentTimeMillis());
                mailJobRepository.save(job);
                log.warn("[MailQueue] Job {} permanently failed after {} attempts", job.getId(), maxAttempts);
            } else {
                // Retry sau 60 giây.
                job.setStatus("retry");
                job.setReservedAt(null);
                job.setAvailableAt(System.currentTimeMillis() + 60_000L);
                job.setUpdatedAt(System.currentTimeMillis());
                mailJobRepository.save(job);
                log.info("[MailQueue] Job {} will retry in 60s (attempt {}/{})", job.getId(), job.getAttempts(), maxAttempts);
            }
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
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
}
