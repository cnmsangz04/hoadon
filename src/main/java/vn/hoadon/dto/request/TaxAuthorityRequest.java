package vn.hoadon.dto.request;

import lombok.Data;

@Data
public class TaxAuthorityRequest {
    private String code;
    private String name;
    private String provinceName;
    private Long parentId;       // Client gửi lên ID cha (nếu có)
    private Integer status;

    // Explicit getters/setters to avoid Lombok issues during compilation
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProvinceName() { return provinceName; }
    public void setProvinceName(String provinceName) { this.provinceName = provinceName; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}