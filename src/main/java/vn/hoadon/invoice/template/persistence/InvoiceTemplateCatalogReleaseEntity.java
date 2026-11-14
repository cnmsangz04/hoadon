package vn.hoadon.invoice.template.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "invoice_template_catalog_releases")
@org.hibernate.annotations.Immutable
public class InvoiceTemplateCatalogReleaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String snapshotJson;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "published_by")
    private Long publishedBy;

    @Column(name = "published_at", nullable = false)
    private java.time.LocalDateTime publishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String value) {
        this.code = value;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String value) {
        this.snapshotJson = value;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String value) {
        this.contentHash = value;
    }

    public Long getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(Long value) {
        this.publishedBy = value;
    }

    public java.time.LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(java.time.LocalDateTime value) {
        this.publishedAt = value;
    }
}
