package vn.hoadon.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "companies")
public class CompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "NVARCHAR(255)")
    private String prefix;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String email;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String hotline;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String info;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(unique = true, columnDefinition = "NVARCHAR(255)")
    private String taxcode;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String address;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String business;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String favicon;

    @Column(name = "invoice_email", columnDefinition = "NVARCHAR(255)")
    private String invoiceEmail;

    @Column(name = "invoice_fax", columnDefinition = "NVARCHAR(255)")
    private String invoiceFax;

    @Column(name = "invoice_phone", columnDefinition = "NVARCHAR(255)")
    private String invoicePhone;

    @Column(name = "invoice_website", columnDefinition = "NVARCHAR(255)")
    private String invoiceWebsite;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String logo;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String name;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // FK Tax Authority (managing authority under a city)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_authority_id")
    private TaxAuthorityEntity taxAuthority;

    // FK Tax Authority City (parent_id = 0)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_authority_city_id")
    private TaxAuthorityEntity taxAuthorityCity;

    @OneToMany(
            mappedBy = "company",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MailTemplateEntity> mailTemplates = new ArrayList<>();

    // One company có nhiều ngân hàng
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompanyBankEntity> companyBanks;

    @Column(name = "contact_name", columnDefinition = "NVARCHAR(255)")
    private String contactName;

    @Column(name = "contact_mail", columnDefinition = "NVARCHAR(255)")
    private String contactMail;

    @Column(name = "contact_phone", columnDefinition = "NVARCHAR(50)")
    private String contactPhone;

    @Column(name = "contact_address", columnDefinition = "NVARCHAR(255)")
    private String contactAddress;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHotline() { return hotline; }
    public void setHotline(String hotline) { this.hotline = hotline; }

    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getTaxcode() { return taxcode; }
    public void setTaxcode(String taxcode) { this.taxcode = taxcode; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getBusiness() { return business; }
    public void setBusiness(String business) { this.business = business; }

    public String getFavicon() { return favicon; }
    public void setFavicon(String favicon) { this.favicon = favicon; }

    public String getInvoiceEmail() { return invoiceEmail; }
    public void setInvoiceEmail(String invoiceEmail) { this.invoiceEmail = invoiceEmail; }

    public String getInvoiceFax() { return invoiceFax; }
    public void setInvoiceFax(String invoiceFax) { this.invoiceFax = invoiceFax; }

    public String getInvoicePhone() { return invoicePhone; }
    public void setInvoicePhone(String invoicePhone) { this.invoicePhone = invoicePhone; }

    public String getInvoiceWebsite() { return invoiceWebsite; }
    public void setInvoiceWebsite(String invoiceWebsite) { this.invoiceWebsite = invoiceWebsite; }

    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public TaxAuthorityEntity getTaxAuthority() { return taxAuthority; }
    public void setTaxAuthority(TaxAuthorityEntity taxAuthority) { this.taxAuthority = taxAuthority; }

    public TaxAuthorityEntity getTaxAuthorityCity() { return taxAuthorityCity; }
    public void setTaxAuthorityCity(TaxAuthorityEntity taxAuthorityCity) { this.taxAuthorityCity = taxAuthorityCity; }

    public List<CompanyBankEntity> getCompanyBanks() { return companyBanks; }
    public void setCompanyBanks(List<CompanyBankEntity> companyBanks) { this.companyBanks = companyBanks; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactMail() { return contactMail; }
    public void setContactMail(String contactMail) { this.contactMail = contactMail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactAddress() { return contactAddress; }
    public void setContactAddress(String contactAddress) { this.contactAddress = contactAddress; }
}
