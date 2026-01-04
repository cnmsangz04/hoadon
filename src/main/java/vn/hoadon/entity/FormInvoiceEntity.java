package vn.hoadon.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "form_invoices")
public class FormInvoiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "name")
    private String name;

    @Column(name = "serial")
    private String serial;

    @Column(name = "form_code")
    private String formCode; // NVARCHAR(50) NOT NULL DEFAULT ''

    @Column(name = "`file`")
    private String file;

    @Column(name = "photo")
    private String photo;

    @Column(name = "system", columnDefinition = "tinyint")
    private Integer system; // 0/1

    @Column(name = "type", columnDefinition = "tinyint")
    private Integer type; // 1: one rate, 2: multi rate

    @Column(name = "category", columnDefinition = "tinyint")
    private Integer category; // 1: VAT, 2: Sales

    @Column(name = "status", columnDefinition = "tinyint")
    private Integer status; // generic status

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }
    public String getFormCode() { return formCode; }
    public void setFormCode(String formCode) { this.formCode = formCode; }
    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }
    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
    public Integer getSystem() { return system; }
    public void setSystem(Integer system) { this.system = system; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Integer getCategory() { return category; }
    public void setCategory(Integer category) { this.category = category; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}