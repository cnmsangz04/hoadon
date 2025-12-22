package vn.hoadon.dto.common;

import jakarta.validation.constraints.NotNull;

public class IdRequestDTO {
    @NotNull
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
