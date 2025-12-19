package vn.hoadon.dto.permission;

import lombok.Data;

@Data
public class PermissionDTO {

    private Long id;

    private String name;

    private String displayName;

    private Integer level;

    private Long categoryId;

    private String description;
}