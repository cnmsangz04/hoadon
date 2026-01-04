package vn.hoadon.dto.tax;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class VatRatesDto {
    private Integer id;
    private Integer userId;
    private String label;
    private Integer code;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public VatRatesDto() {
    }
}
