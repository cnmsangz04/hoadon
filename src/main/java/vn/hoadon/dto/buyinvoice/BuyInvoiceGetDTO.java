package vn.hoadon.dto.buyinvoice;

import jakarta.validation.constraints.NotNull;

public class BuyInvoiceGetDTO {

    @NotNull(message = "ID không được để trống")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
