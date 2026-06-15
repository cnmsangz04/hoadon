package vn.hoadon.dto.invoicepackage;

import java.math.BigDecimal;

public class InvoicePackageRequestDTO {
    private Long id;
    private String name;
    private Integer invoiceQuantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Boolean includeTrial;
    private String description;
    private Integer status;
    private Integer displayOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getInvoiceQuantity() { return invoiceQuantity; }
    public void setInvoiceQuantity(Integer invoiceQuantity) { this.invoiceQuantity = invoiceQuantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public Boolean getIncludeTrial() { return includeTrial; }
    public void setIncludeTrial(Boolean includeTrial) { this.includeTrial = includeTrial; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
