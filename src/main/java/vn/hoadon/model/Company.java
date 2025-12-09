package vn.hoadon.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "domain", nullable = false, length = 255, unique = true)
    private String domain;

    @Column(name = "domain_lookup", length = 255)
    private String domainLookup;

    @Column(name = "prefix", nullable = false, length = 255, unique = true)
    private String prefix;

    @Column(name = "pa_uid")
    private Integer paUid;

    @Column(name = "pa_admin")
    private Integer paAdmin;

    @Column(name = "taxcode", length = 16)
    private String taxcode;

    @Column(name = "hotline", length = 32)
    private String hotline;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "status", length = 10)
    private String status = "active";

    @Column(name = "info", columnDefinition = "nvarchar(max)")
    private String info;

    @Column(name = "bank", columnDefinition = "nvarchar(max)")
    private String bank;

    @Column(name = "amount")
    private Integer amount = 0;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getDomainLookup() { return domainLookup; }
    public void setDomainLookup(String domainLookup) { this.domainLookup = domainLookup; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public Integer getPaUid() { return paUid; }
    public void setPaUid(Integer paUid) { this.paUid = paUid; }
    public Integer getPaAdmin() { return paAdmin; }
    public void setPaAdmin(Integer paAdmin) { this.paAdmin = paAdmin; }
    public String getTaxcode() { return taxcode; }
    public void setTaxcode(String taxcode) { this.taxcode = taxcode; }
    public String getHotline() { return hotline; }
    public void setHotline(String hotline) { this.hotline = hotline; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }
    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
