package vn.hoadon.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tax_authorities")
public class TaxAuthorityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer code;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    private Integer status;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "taxAuthority")
    private List<CompanyEntity> companies;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<CompanyEntity> getCompanies() { return companies; }
    public void setCompanies(List<CompanyEntity> companies) { this.companies = companies; }
}
