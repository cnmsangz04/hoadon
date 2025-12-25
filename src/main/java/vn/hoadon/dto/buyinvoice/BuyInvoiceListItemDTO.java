package vn.hoadon.dto.buyinvoice;

import java.time.LocalDateTime;

public class BuyInvoiceListItemDTO {
    private Long id;
    private Long companyId;
    private String companyName;
    private Integer amount;
    private Integer amountUsed;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BuyInvoiceListItemDTO() {}

    public BuyInvoiceListItemDTO(Long id, Long companyId, String companyName, Integer amount, Integer amountUsed, Integer status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.companyName = companyName;
        this.amount = amount;
        this.amountUsed = amountUsed;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public Integer getAmountUsed() { return amountUsed; }
    public void setAmountUsed(Integer amountUsed) { this.amountUsed = amountUsed; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
