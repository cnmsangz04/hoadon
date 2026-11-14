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
@Table(name = "company_invoice_defaults", uniqueConstraints = {@UniqueConstraint(columnNames = {"company_id", "category"})})
public class CompanyInvoiceDefaultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "category", nullable = false)
    private Integer category;

    @Column(name = "activation_policy", nullable = false, length = 40)
    private String activationPolicy;

    @Column(name = "form_invoice_id", nullable = true)
    private Long formInvoiceId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Integer getCategory() { return category; }
    public void setCategory(Integer category) { this.category = category; }
    public String getActivationPolicy() { return activationPolicy; }
    public void setActivationPolicy(String activationPolicy) { this.activationPolicy = activationPolicy; }
    public Long getFormInvoiceId() { return formInvoiceId; }
    public void setFormInvoiceId(Long formInvoiceId) { this.formInvoiceId = formInvoiceId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getLockVersion() { return lockVersion; }
    public void setLockVersion(Long lockVersion) { this.lockVersion = lockVersion; }
}
