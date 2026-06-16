package vn.hoadon.controllers.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.MailJobEntity;
import vn.hoadon.entity.MailTemplateEntity;
import vn.hoadon.messaging.MailJobMessage;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.MailJobRepository;
import vn.hoadon.repositories.MailTemplateRepository;
import vn.hoadon.services.MailQueueService;
import vn.hoadon.util.SystemMail;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/administrator/mail-jobs")
public class MailJobAdminController {

    @Autowired private MailJobRepository mailJobRepository;
    @Autowired private MailTemplateRepository mailTemplateRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private MailQueueService mailQueueService;
    @Autowired private ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(name = "q", required = false) String q,
                                  @RequestParam(name = "status", required = false) String status,
                                  @RequestParam(name = "page", defaultValue = "1") int page,
                                  @RequestParam(name = "size", defaultValue = "10") int size) {
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        String keyword = q == null || q.trim().isEmpty() ? null : q.trim();
        String statusFilter = status == null || status.trim().isEmpty() ? null : status.trim();

        Page<MailJobEntity> result = mailJobRepository.searchAllHistory(statusFilter, keyword, pageable);
        List<Map<String, Object>> items = new ArrayList<>();
        for (MailJobEntity job : result.getContent()) {
            items.add(toRow(job));
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("items", items);
        resp.put("current_page", result.getNumber() + 1);
        resp.put("per_page", result.getSize());
        resp.put("total", result.getTotalElements());
        resp.put("last_page", result.getTotalPages());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<?> retry(@PathVariable("id") Long id) {
        MailJobEntity oldJob = mailJobRepository.findById(id).orElse(null);
        if (oldJob == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy lịch sử gửi mail"));
        }
        if (oldJob.getPayload() == null || oldJob.getPayload().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không có dữ liệu email để gửi lại"));
        }
        try {
            MailJobMessage message = objectMapper.readValue(oldJob.getPayload(), MailJobMessage.class);
            if (message.getCompanyId() == null) message.setCompanyId(oldJob.getCompanyId());
            if (message.getInvoiceId() == null) message.setInvoiceId(oldJob.getInvoiceId());
            if (message.getTemplateKey() == null) message.setTemplateKey(oldJob.getTemplateKey());
            if (message.getToEmail() == null) message.setToEmail(oldJob.getToEmail());
            if (message.getToName() == null) message.setToName(oldJob.getToName());
            mailQueueService.enqueue(message);
            return ResponseEntity.ok(Map.of("message", "Đã đưa email vào hàng đợi gửi lại"));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("message", "Không thể gửi lại email: " + ex.getMessage()));
        }
    }

    private Map<String, Object> toRow(MailJobEntity job) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", job.getId());
        row.put("companyId", job.getCompanyId());
        row.put("companyName", resolveCompanyName(job.getCompanyId()));
        row.put("invoiceId", job.getInvoiceId());
        row.put("templateKey", job.getTemplateKey());
        row.put("templateName", resolveTemplateName(job));
        row.put("toEmail", job.getToEmail());
        row.put("toName", job.getToName());
        row.put("subject", job.getSubject());
        row.put("status", job.getStatus());
        row.put("attempts", job.getAttempts());
        row.put("showHistory", job.isShowHistory());
        row.put("failed", job.isFailed());
        row.put("error", job.getError());
        row.put("createdAt", toDateTime(job.getCreatedAt()));
        row.put("updatedAt", toDateTime(job.getUpdatedAt()));
        row.put("lastAttemptAt", toDateTime(job.getLastAttemptAt()));
        row.put("sentAt", toDateTime(job.getSentAt()));
        row.put("failedAt", toDateTime(job.getFailedAt()));
        return row;
    }

    private String resolveCompanyName(Long companyId) {
        if (companyId == null) return null;
        return companyRepository.findById(companyId)
                .map(CompanyEntity::getName)
                .orElse(null);
    }

    private String resolveTemplateName(MailJobEntity job) {
        if (job == null || job.getTemplateKey() == null || job.getTemplateKey().isBlank()) {
            return null;
        }

        Long templateCompanyId = SystemMail.resolveCompanyId(job.getTemplateKey(), job.getCompanyId());
        if (templateCompanyId != null) {
            MailTemplateEntity tpl = mailTemplateRepository.findByKeyAndCompanyId(
                    job.getTemplateKey(), templateCompanyId.intValue());
            if (tpl != null && tpl.getStatus() != null && tpl.getStatus() == 1
                    && tpl.getTitle() != null && !tpl.getTitle().isBlank()) {
                return tpl.getTitle();
            }
        }

        MailTemplateEntity sysTpl = mailTemplateRepository.findSystemByKey(job.getTemplateKey());
        if (sysTpl != null && sysTpl.getStatus() != null && sysTpl.getStatus() == 1
                && sysTpl.getTitle() != null && !sysTpl.getTitle().isBlank()) {
            return sysTpl.getTitle();
        }

        if ("ISSUE_INVOICE_MAIL".equals(job.getTemplateKey())) return "Thông báo phát hành hóa đơn";
        if ("ACCOUNT_INFO_MAIL".equals(job.getTemplateKey())) return "Gửi thông tin tài khoản";
        if ("LOGIN_INFO_MAIL".equals(job.getTemplateKey())) return "Gửi thông tin đăng nhập";
        if ("RESET_PASSWORD_MAIL".equals(job.getTemplateKey())) return "Đặt lại mật khẩu";
        return job.getTemplateKey();
    }

    private LocalDateTime toDateTime(Long epochMs) {
        if (epochMs == null) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
    }
}
