package vn.hoadon.invoice.template.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "invoice_template_version_assets", uniqueConstraints = {@UniqueConstraint(columnNames = {"template_version_id", "logical_name"})})
@Immutable
public class InvoiceTemplateVersionAssetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_version_id", nullable = false)
    private Long templateVersionId;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "logical_name", nullable = false, length = 128)
    private String logicalName;

    @Column(name = "usage", nullable = false, length = 24)
    private String usage;

    @Column(name = "expected_sha256", nullable = false, length = 64)
    private String expectedSha256;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTemplateVersionId() { return templateVersionId; }
    public void setTemplateVersionId(Long templateVersionId) { this.templateVersionId = templateVersionId; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public String getLogicalName() { return logicalName; }
    public void setLogicalName(String logicalName) { this.logicalName = logicalName; }
    public String getUsage() { return usage; }
    public void setUsage(String usage) { this.usage = usage; }
    public String getExpectedSha256() { return expectedSha256; }
    public void setExpectedSha256(String expectedSha256) { this.expectedSha256 = expectedSha256; }
}
