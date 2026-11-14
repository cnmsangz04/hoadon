package vn.hoadon.invoice.template.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_template_validation_runs")
public class InvoiceTemplateValidationRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "draft_id", nullable = false)
    private Long draftId;

    @Column(name = "draft_lock_version", nullable = false)
    private Long draftLockVersion;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "manifest_hash", nullable = false, length = 64)
    private String manifestHash;

    @Column(name = "profile_version", nullable = false, length = 64)
    private String profileVersion;

    @Column(name = "job_ids_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String jobIdsJson;

    @Column(name = "state", nullable = false, length = 16)
    private String state;

    @Column(name = "report_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String reportJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDraftId() { return draftId; }
    public void setDraftId(Long draftId) { this.draftId = draftId; }
    public Long getDraftLockVersion() { return draftLockVersion; }
    public void setDraftLockVersion(Long draftLockVersion) { this.draftLockVersion = draftLockVersion; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getManifestHash() { return manifestHash; }
    public void setManifestHash(String manifestHash) { this.manifestHash = manifestHash; }
    public String getProfileVersion() { return profileVersion; }
    public void setProfileVersion(String profileVersion) { this.profileVersion = profileVersion; }
    public String getJobIdsJson() { return jobIdsJson; }
    public void setJobIdsJson(String jobIdsJson) { this.jobIdsJson = jobIdsJson; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getReportJson() { return reportJson; }
    public void setReportJson(String reportJson) { this.reportJson = reportJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
