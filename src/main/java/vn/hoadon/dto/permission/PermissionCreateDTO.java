package vn.hoadon.dto.permission;

import lombok.Data;

@Data
public class PermissionCreateDTO {
    private Long id;
    private Long category;
    private String name;
    private Integer level;
    private String displayName;
    private String description;
}
