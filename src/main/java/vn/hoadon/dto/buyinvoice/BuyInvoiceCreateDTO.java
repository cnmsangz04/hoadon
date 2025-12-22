package vn.hoadon.dto.buyinvoice;

public class BuyInvoiceCreateDTO {
    private Long id;
    private Long companyId;
    private Integer amount;
    private Integer status;

    // getter / setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
