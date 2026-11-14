package vn.hoadon.invoice.template.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_template_import_jobs")
public class InvoiceTemplateImportJobEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "source_asset_id", nullable = false)
    private Long sourceAssetId;

    @Column(name = "target_template_id", nullable = true)
    private Long targetTemplateId;

    @Column(name = "expected_draft_lock_version", nullable = true)
    private Long expectedDraftLockVersion;

    @Column(name = "state", nullable = false, length = 24)
    private String state;

    @Column(name = "provider", nullable = true, length = 64)
    private String provider;

    @Column(name = "model", nullable = true, length = 100)
    private String model;

    @Column(name = "proposal_json", nullable = true, columnDefinition = "NVARCHAR(MAX)")
    private String proposalJson;

    @Column(name = "error_code", nullable = true, length = 64)
    private String errorCode;

    @Column(name = "applied_draft_id", nullable = true)
    private Long appliedDraftId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getSourceAssetId() { return sourceAssetId; }
    public void setSourceAssetId(Long sourceAssetId) { this.sourceAssetId = sourceAssetId; }
    public Long getTargetTemplateId() { return targetTemplateId; }
    public void setTargetTemplateId(Long targetTemplateId) { this.targetTemplateId = targetTemplateId; }
    public Long getExpectedDraftLockVersion() { return expectedDraftLockVersion; }
    public void setExpectedDraftLockVersion(Long expectedDraftLockVersion) { this.expectedDraftLockVersion = expectedDraftLockVersion; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getProposalJson() { return proposalJson; }
    public void setProposalJson(String proposalJson) { this.proposalJson = proposalJson; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public Long getAppliedDraftId() { return appliedDraftId; }
    public void setAppliedDraftId(Long appliedDraftId) { this.appliedDraftId = appliedDraftId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getLockVersion() { return lockVersion; }
    public void setLockVersion(Long lockVersion) { this.lockVersion = lockVersion; }
}
