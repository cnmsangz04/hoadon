package vn.hoadon.invoice.template.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_render_jobs", uniqueConstraints = {@UniqueConstraint(columnNames = {"request_key"})})
public class InvoiceRenderJobEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope", nullable = false, length = 16)
    private String scope;

    @Column(name = "company_id", nullable = true)
    private Long companyId;

    @Column(name = "purpose", nullable = false, length = 24)
    private String purpose;

    @Column(name = "template_version_id", nullable = true)
    private Long templateVersionId;

    @Column(name = "draft_id", nullable = true)
    private Long draftId;

    @Column(name = "draft_lock_version", nullable = true)
    private Long draftLockVersion;

    @Column(name = "snapshot_id", nullable = true)
    private Long snapshotId;

    @Column(name = "request_key", nullable = false, length = 64)
    private String requestKey;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(name = "source_content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String sourceContent;

    @Column(name = "source_xml", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String sourceXml;

    @Column(name = "manifest_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String manifestJson;

    @Column(name = "parameters_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String parametersJson;

    @Column(name = "profile_version", nullable = false, length = 64)
    private String profileVersion;

    @Column(name = "state", nullable = false, length = 16)
    private String state;

    @Column(name = "claim_token", nullable = true, length = 36)
    private String claimToken;

    @Column(name = "lease_until", nullable = true)
    private LocalDateTime leaseUntil;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "artifact_id", nullable = true)
    private Long artifactId;

    @Column(name = "error_code", nullable = true, length = 64)
    private String errorCode;

    @Column(name = "error_message", nullable = true, columnDefinition = "NVARCHAR(1000)")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at", nullable = true)
    private LocalDateTime completedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public Long getTemplateVersionId() { return templateVersionId; }
    public void setTemplateVersionId(Long templateVersionId) { this.templateVersionId = templateVersionId; }
    public Long getDraftId() { return draftId; }
    public void setDraftId(Long draftId) { this.draftId = draftId; }
    public Long getDraftLockVersion() { return draftLockVersion; }
    public void setDraftLockVersion(Long draftLockVersion) { this.draftLockVersion = draftLockVersion; }
    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }
    public String getInputHash() { return inputHash; }
    public void setInputHash(String inputHash) { this.inputHash = inputHash; }
    public String getSourceContent() { return sourceContent; }
    public void setSourceContent(String sourceContent) { this.sourceContent = sourceContent; }
    public String getSourceXml() { return sourceXml; }
    public void setSourceXml(String sourceXml) { this.sourceXml = sourceXml; }
    public String getManifestJson() { return manifestJson; }
    public void setManifestJson(String manifestJson) { this.manifestJson = manifestJson; }
    public String getParametersJson() { return parametersJson; }
    public void setParametersJson(String parametersJson) { this.parametersJson = parametersJson; }
    public String getProfileVersion() { return profileVersion; }
    public void setProfileVersion(String profileVersion) { this.profileVersion = profileVersion; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getClaimToken() { return claimToken; }
    public void setClaimToken(String claimToken) { this.claimToken = claimToken; }
    public LocalDateTime getLeaseUntil() { return leaseUntil; }
    public void setLeaseUntil(LocalDateTime leaseUntil) { this.leaseUntil = leaseUntil; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public Long getArtifactId() { return artifactId; }
    public void setArtifactId(Long artifactId) { this.artifactId = artifactId; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Long getLockVersion() { return lockVersion; }
    public void setLockVersion(Long lockVersion) { this.lockVersion = lockVersion; }
}
