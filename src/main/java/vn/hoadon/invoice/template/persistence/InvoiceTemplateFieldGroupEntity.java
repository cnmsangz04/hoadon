package vn.hoadon.invoice.template.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "invoice_template_field_groups")
public class InvoiceTemplateFieldGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "label", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String label;

    @Column(name = "cardinality", nullable = false, length = 8)
    private String cardinality;

    @Column(name = "root_path", length = 700)
    private String rootPath;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLockVersion() {
        return lockVersion;
    }

    public void setLockVersion(Long value) {
        this.lockVersion = value;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String value) {
        this.code = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String value) {
        this.label = value;
    }

    public String getCardinality() {
        return cardinality;
    }

    public void setCardinality(String value) {
        this.cardinality = value;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String value) {
        this.rootPath = value;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int value) {
        this.sortOrder = value;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean value) {
        this.active = value;
    }
}
