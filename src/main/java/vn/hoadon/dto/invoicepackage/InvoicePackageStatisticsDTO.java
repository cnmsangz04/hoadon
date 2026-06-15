package vn.hoadon.dto.invoicepackage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InvoicePackageStatisticsDTO {
    private Long totalOrders = 0L;
    private Integer totalInvoices = 0;
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private List<InvoicePackageMonthlyStatisticDTO> monthly = new ArrayList<>();

    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

    public Integer getTotalInvoices() { return totalInvoices; }
    public void setTotalInvoices(Integer totalInvoices) { this.totalInvoices = totalInvoices; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public List<InvoicePackageMonthlyStatisticDTO> getMonthly() { return monthly; }
    public void setMonthly(List<InvoicePackageMonthlyStatisticDTO> monthly) { this.monthly = monthly; }
}
