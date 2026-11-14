package vn.hoadon.invoice.template.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "invoice_template_versions", uniqueConstraints = {@UniqueConstraint(columnNames = {"template_id", "version_no"})})
@Immutable
public class InvoiceTemplateVersionEntity {
    @Column(name = "capabilities_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String capabilitiesJson;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "source_content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String sourceContent;

    @Column(name = "manifest_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String manifestJson;

    @Column(name = "schema_version", nullable = false, length = 32)
    private String schemaVersion;

    @Column(name = "field_catalog_version", nullable = false, length = 64)
    private String fieldCatalogVersion;

    @Column(name = "compiler_version", nullable = false, length = 64)
    private String compilerVersion;

    @Column(name = "render_profile_version", nullable = false, length = 64)
    private String renderProfileVersion;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "manifest_hash", nullable = false, length = 64)
    private String manifestHash;

    @Column(name = "validation_report_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String validationReportJson;

    @Column(name = "published_by", nullable = true)
    private Long publishedBy;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    public Long getId() { return id; }
    public String getCapabilitiesJson() { return capabilitiesJson; }
    public void setCapabilitiesJson(String capabilitiesJson) { this.capabilitiesJson = capabilitiesJson; }
    public void setId(Long id) { this.id = id; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getSourceContent() { return sourceContent; }
    public void setSourceContent(String sourceContent) { this.sourceContent = sourceContent; }
    public String getManifestJson() { return manifestJson; }
    public void setManifestJson(String manifestJson) { this.manifestJson = manifestJson; }
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getFieldCatalogVersion() { return fieldCatalogVersion; }
    public void setFieldCatalogVersion(String fieldCatalogVersion) { this.fieldCatalogVersion = fieldCatalogVersion; }
    public String getCompilerVersion() { return compilerVersion; }
    public void setCompilerVersion(String compilerVersion) { this.compilerVersion = compilerVersion; }
    public String getRenderProfileVersion() { return renderProfileVersion; }
    public void setRenderProfileVersion(String renderProfileVersion) { this.renderProfileVersion = renderProfileVersion; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getManifestHash() { return manifestHash; }
    public void setManifestHash(String manifestHash) { this.manifestHash = manifestHash; }
    public String getValidationReportJson() { return validationReportJson; }
    public void setValidationReportJson(String validationReportJson) { this.validationReportJson = validationReportJson; }
    public Long getPublishedBy() { return publishedBy; }
    public void setPublishedBy(Long publishedBy) { this.publishedBy = publishedBy; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
}
