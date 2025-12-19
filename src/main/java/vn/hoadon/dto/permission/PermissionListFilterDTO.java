package vn.hoadon.dto.permission;

import lombok.Data;

@Data
public class PermissionListFilterDTO {

    private Long categoryId;

    private Integer level;

    private String keyword;
}
