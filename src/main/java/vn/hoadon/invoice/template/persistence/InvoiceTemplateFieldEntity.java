package vn.hoadon.invoice.template.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "invoice_template_fields",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "UK_invoice_template_fields_key",
                        columnNames = "field_key"))
public class InvoiceTemplateFieldEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;

    @Column(name = "field_key", nullable = false, length = 160)
    private String fieldKey;

    @Column(name = "label", nullable = false, columnDefinition = "NVARCHAR(500)")
    private String label;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(name = "source_path", nullable = false, length = 700)
    private String sourcePath;

    @Column(name = "value_type", nullable = false, length = 16)
    private String valueType;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "description", columnDefinition = "NVARCHAR(2000)")
    private String description;

    @Column(name = "default_formatter", nullable = false, length = 40)
    private String defaultFormatter;

    @Column(name = "required_display", nullable = false)
    private boolean requiredDisplay;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getLockVersion() {
        return lockVersion;
    }

    public void setLockVersion(Long value) {
        this.lockVersion = value;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String value) {
        this.fieldKey = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String value) {
        this.label = value;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long value) {
        this.groupId = value;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Long value) {
        this.datasetId = value;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String value) {
        this.sourceType = value;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String value) {
        this.sourcePath = value;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String value) {
        this.valueType = value;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer value) {
        this.maxLength = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public String getDefaultFormatter() {
        return defaultFormatter;
    }

    public void setDefaultFormatter(String value) {
        this.defaultFormatter = value;
    }

    public boolean isRequiredDisplay() {
        return requiredDisplay;
    }

    public void setRequiredDisplay(boolean value) {
        this.requiredDisplay = value;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean value) {
        this.active = value;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int value) {
        this.sortOrder = value;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long value) {
        this.createdBy = value;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long value) {
        this.updatedBy = value;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime value) {
        this.createdAt = value;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime value) {
        this.updatedAt = value;
    }
}
