package vn.hoadon.dto.request;

import lombok.Data;

@Data
public class TaxAuthorityRequest {
    private String code;
    private String name;
    private String provinceName;
    private Long parentId;       // Client gửi lên ID cha (nếu có)
    private Integer status;
}
