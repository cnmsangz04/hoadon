package vn.hoadon.invoice.template.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "invoice_document_snapshots")
@Immutable
public class InvoiceDocumentSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "template_version_id", nullable = false)
    private Long templateVersionId;

    @Column(name = "source_stage", nullable = false, length = 32)
    private String sourceStage;

    @Column(name = "source_xml", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String sourceXml;

    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @Column(name = "context_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String contextJson;

    @Column(name = "fidelity", nullable = false, length = 32)
    private String fidelity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getTemplateVersionId() { return templateVersionId; }
    public void setTemplateVersionId(Long templateVersionId) { this.templateVersionId = templateVersionId; }
    public String getSourceStage() { return sourceStage; }
    public void setSourceStage(String sourceStage) { this.sourceStage = sourceStage; }
    public String getSourceXml() { return sourceXml; }
    public void setSourceXml(String sourceXml) { this.sourceXml = sourceXml; }
    public String getSourceHash() { return sourceHash; }
    public void setSourceHash(String sourceHash) { this.sourceHash = sourceHash; }
    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }
    public String getFidelity() { return fidelity; }
    public void setFidelity(String fidelity) { this.fidelity = fidelity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
