package vn.hoadon.entity;

import jakarta.persistence.*;
import vn.hoadon.entity.converters.LongListJsonConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hash_invoices")
public class HashInvoiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name_user", length = 100)
    private String nameUser;

    @Column(name = "hash", nullable = false, unique = true, length = 80)
    private String hash;

    @Convert(converter = LongListJsonConverter.class)
    @Column(name = "ids", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private List<Long> ids = new ArrayList<>();

    @Column(name = "id_attr", length = 100)
    private String idAttr;

    @Column(name = "sign_by", length = 50)
    private String signBy;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNameUser() { return nameUser; }
    public void setNameUser(String nameUser) { this.nameUser = nameUser; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids != null ? ids : new ArrayList<>(); }
    public String getIdAttr() { return idAttr; }
    public void setIdAttr(String idAttr) { this.idAttr = idAttr; }
    public String getSignBy() { return signBy; }
    public void setSignBy(String signBy) { this.signBy = signBy; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
