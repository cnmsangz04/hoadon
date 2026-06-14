package vn.hoadon.controllers.customers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.MailJobEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.MailJobRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/mail-jobs")
public class MailJobController extends BaseController {

    @Autowired private MailJobRepository mailJobRepository;

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal UserEntity user,
                                  @RequestParam(name = "q", required = false) String q,
                                  @RequestParam(name = "status", required = false) String status,
                                  @RequestParam(name = "page", defaultValue = "1") int page,
                                  @RequestParam(name = "size", defaultValue = "10") int size) {
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(403).build();
        }

        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        String keyword = q == null || q.trim().isEmpty() ? null : q.trim();
        String statusFilter = status == null || status.trim().isEmpty() ? null : status.trim();

        Page<MailJobEntity> result = mailJobRepository.searchHistory(user.getCompanyId(), statusFilter, keyword, pageable);
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

    private Map<String, Object> toRow(MailJobEntity job) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", job.getId());
        row.put("invoiceId", job.getInvoiceId());
        row.put("templateKey", job.getTemplateKey());
        row.put("toEmail", job.getToEmail());
        row.put("toName", job.getToName());
        row.put("subject", job.getSubject());
        row.put("status", job.getStatus());
        row.put("attempts", job.getAttempts());
        row.put("error", job.getError());
        row.put("createdAt", toDateTime(job.getCreatedAt()));
        row.put("updatedAt", toDateTime(job.getUpdatedAt()));
        row.put("lastAttemptAt", toDateTime(job.getLastAttemptAt()));
        row.put("sentAt", toDateTime(job.getSentAt()));
        row.put("failedAt", toDateTime(job.getFailedAt()));
        return row;
    }

    private LocalDateTime toDateTime(Long epochMs) {
        if (epochMs == null) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
    }
}
