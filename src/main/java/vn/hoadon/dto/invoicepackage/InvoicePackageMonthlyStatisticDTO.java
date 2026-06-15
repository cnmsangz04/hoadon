package vn.hoadon.dto.invoicepackage;

import java.math.BigDecimal;

public class InvoicePackageMonthlyStatisticDTO {
    private String month;
    private Long orderCount;
    private Integer invoiceQuantity;
    private BigDecimal revenue;

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public Long getOrderCount() { return orderCount; }
    public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }

    public Integer getInvoiceQuantity() { return invoiceQuantity; }
    public void setInvoiceQuantity(Integer invoiceQuantity) { this.invoiceQuantity = invoiceQuantity; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
}
