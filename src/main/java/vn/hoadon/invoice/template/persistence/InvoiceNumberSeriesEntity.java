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
@Table(name = "invoice_number_series", uniqueConstraints = {@UniqueConstraint(columnNames = {"company_id", "form_code", "serial"})})
public class InvoiceNumberSeriesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "category", nullable = false)
    private Integer category;

    @Column(name = "form_code", nullable = false, length = 1)
    private String formCode;

    @Column(name = "serial", nullable = false, length = 6)
    private String serial;

    @Column(name = "have_code", nullable = false)
    private Boolean haveCode;

    @Column(name = "tax_mode", nullable = false, length = 16)
    private String taxMode;

    @Column(name = "last_no", nullable = false)
    private Long lastNo;

    @Column(name = "state", nullable = false, length = 16)
    private String state;

    @Column(name = "rule_version", nullable = false, length = 64)
    private String ruleVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Integer getCategory() { return category; }
    public void setCategory(Integer category) { this.category = category; }
    public String getFormCode() { return formCode; }
    public void setFormCode(String formCode) { this.formCode = formCode; }
    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }
    public Boolean getHaveCode() { return haveCode; }
    public void setHaveCode(Boolean haveCode) { this.haveCode = haveCode; }
    public String getTaxMode() { return taxMode; }
    public void setTaxMode(String taxMode) { this.taxMode = taxMode; }
    public Long getLastNo() { return lastNo; }
    public void setLastNo(Long lastNo) { this.lastNo = lastNo; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getLockVersion() { return lockVersion; }
    public void setLockVersion(Long lockVersion) { this.lockVersion = lockVersion; }
}
