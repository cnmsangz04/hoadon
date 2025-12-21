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
}
