package vn.hoadon.ai.invoice.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_invoice_proposals", indexes = {
        @Index(name = "ix_ai_invoice_proposal_owner", columnList = "user_id,company_id,created_at"),
        @Index(name = "ix_ai_invoice_proposal_invoice", columnList = "invoice_id,status")
})
public class InvoiceAiProposalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    @Column(nullable = false)
    private Long version;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    @Column(name = "invoice_id")
    private Long invoiceId;
    @Column(name = "form_id", nullable = false)
    private Long formId;
    @Column(name = "invoice_category", nullable = false)
    private Integer invoiceCategory;
    @Column(name = "command_text", nullable = false, length = 2000, columnDefinition = "NVARCHAR(2000)")
    private String commandText;
    @Column(name = "actions_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String actionsJson;
    @Column(name = "baseline_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String baselineJson;
    @Column(name = "result_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String resultJson;
    @Column(name = "baseline_hash", nullable = false, length = 64)
    private String baselineHash;
    @Column(name = "baseline_updated_at")
    private LocalDateTime baselineUpdatedAt;
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    @Column(name = "request_id", length = 64)
    private String requestId;
    @Column(name = "applied_invoice_id")
    private Long appliedInvoiceId;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public Long getFormId() { return formId; }
    public void setFormId(Long formId) { this.formId = formId; }
    public Integer getInvoiceCategory() { return invoiceCategory; }
    public void setInvoiceCategory(Integer invoiceCategory) { this.invoiceCategory = invoiceCategory; }
    public String getCommandText() { return commandText; }
    public void setCommandText(String commandText) { this.commandText = commandText; }
    public String getActionsJson() { return actionsJson; }
    public void setActionsJson(String actionsJson) { this.actionsJson = actionsJson; }
    public String getBaselineJson() { return baselineJson; }
    public void setBaselineJson(String baselineJson) { this.baselineJson = baselineJson; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public String getBaselineHash() { return baselineHash; }
    public void setBaselineHash(String baselineHash) { this.baselineHash = baselineHash; }
    public LocalDateTime getBaselineUpdatedAt() { return baselineUpdatedAt; }
    public void setBaselineUpdatedAt(LocalDateTime baselineUpdatedAt) { this.baselineUpdatedAt = baselineUpdatedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getAppliedInvoiceId() { return appliedInvoiceId; }
    public void setAppliedInvoiceId(Long appliedInvoiceId) { this.appliedInvoiceId = appliedInvoiceId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
