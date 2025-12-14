package vn.hoadon.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
public class CompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String domain;

    @Column(name = "domain_lookup")
    private String domainLookup;

    @Column(nullable = false, unique = true)
    private String prefix;

    private String taxcode;
    private String email;
    private String hotline;
    private String password;

    @Column(nullable = false)
    private String status = "active";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getter / Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getDomainLookup() { return domainLookup; }
    public void setDomainLookup(String domainLookup) { this.domainLookup = domainLookup; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public String getTaxcode() { return taxcode; }
    public void setTaxcode(String taxcode) { this.taxcode = taxcode; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHotline() { return hotline; }
    public void setHotline(String hotline) { this.hotline = hotline; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
