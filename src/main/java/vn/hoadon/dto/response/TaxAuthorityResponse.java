package vn.hoadon.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TaxAuthorityResponse {
    private Long id;
    private String code;
    private String name;
    private String provinceName;
    private Long parentId;       // ID cấp cha
    private String managerName;  // Tên cấp cha (để hiển thị cột "Cơ quan quản lý")
    private Integer status;
    private LocalDateTime createdAt;

    // Explicit getters/setters to avoid Lombok issues during compilation
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProvinceName() { return provinceName; }
    public void setProvinceName(String provinceName) { this.provinceName = provinceName; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}