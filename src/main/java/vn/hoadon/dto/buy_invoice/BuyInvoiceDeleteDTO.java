package vn.hoadon.dto.buy_invoice;

import jakarta.validation.constraints.NotNull;

public class BuyInvoiceDeleteDTO {

    @NotNull(message = "ID không được để trống")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
