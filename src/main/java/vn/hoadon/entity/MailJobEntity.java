package vn.hoadon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Job hàng đợi mail, tương tự bảng jobs của bộ xử lý hàng đợi cơ sở dữ liệu.
 * Hibernate tự tạo bảng mail_jobs khi ddl-auto = update.
 */
@Entity
@Table(name = "mail_jobs")
public class MailJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "queue", nullable = false, length = 255)
    private String queue = "mail";

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "template_key", length = 100)
    private String templateKey;

    @Column(name = "to_email", length = 255)
    private String toEmail;

    @Column(name = "to_name", columnDefinition = "NVARCHAR(255)")
    private String toName;

    @Column(name = "subject", columnDefinition = "NVARCHAR(500)")
    private String subject;

    @Column(name = "show_history", nullable = false)
    private boolean showHistory = true;

    @Column(name = "status", length = 30)
    private String status = "queued";

    /** Chuỗi JSON của MailJobMessage. */
    @Column(name = "payload", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String payload;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    /** Epoch ms, null khi job chưa được xử lý. */
    @Column(name = "reserved_at")
    private Long reservedAt;

    /** Epoch ms, job sẵn sàng xử lý khi currentTime >= availableAt. */
    @Column(name = "available_at", nullable = false)
    private Long availableAt;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;

    @Column(name = "last_attempt_at")
    private Long lastAttemptAt;

    @Column(name = "sent_at")
    private Long sentAt;

    @Column(name = "failed_at")
    private Long failedAt;

    /** true khi đã thất bại quá số lần retry cho phép. */
    @Column(name = "failed", nullable = false)
    private boolean failed = false;

    /** Lỗi cuối cùng khi job thất bại vĩnh viễn. */
    @Column(name = "error", columnDefinition = "NVARCHAR(MAX)")
    private String error;

    // Getter / setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQueue() { return queue; }
    public void setQueue(String queue) { this.queue = queue; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long i) { this.invoiceId = i; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long c) { this.companyId = c; }

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String t) { this.templateKey = t; }

    public String getToEmail() { return toEmail; }
    public void setToEmail(String e) { this.toEmail = e; }

    public String getToName() { return toName; }
    public void setToName(String n) { this.toName = n; }

    public String getSubject() { return subject; }
    public void setSubject(String s) { this.subject = s; }

    public boolean isShowHistory() { return showHistory; }
    public void setShowHistory(boolean s) { this.showHistory = s; }

    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }

    public String getPayload() { return payload; }
    public void setPayload(String p) { this.payload = p; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int a) { this.attempts = a; }

    public Long getReservedAt() { return reservedAt; }
    public void setReservedAt(Long r) { this.reservedAt = r; }

    public Long getAvailableAt() { return availableAt; }
    public void setAvailableAt(Long a) { this.availableAt = a; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long c) { this.createdAt = c; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long u) { this.updatedAt = u; }

    public Long getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Long l) { this.lastAttemptAt = l; }

    public Long getSentAt() { return sentAt; }
    public void setSentAt(Long s) { this.sentAt = s; }

    public Long getFailedAt() { return failedAt; }
    public void setFailedAt(Long f) { this.failedAt = f; }

    public boolean isFailed() { return failed; }
    public void setFailed(boolean f) { this.failed = f; }

    public String getError() { return error; }
    public void setError(String e) { this.error = e; }
}
