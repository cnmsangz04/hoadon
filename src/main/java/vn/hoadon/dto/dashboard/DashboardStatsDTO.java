package vn.hoadon.dto.dashboard;

public class DashboardStatsDTO {
    private Integer totalInvoices;           // Tổng số hóa đơn đã mua (buy_invoices.amount)
    private Integer usedInvoices;            // Số hóa đơn đã sử dụng (buy_invoices.amount_used)
    private Integer remainingInvoices;       // Số hóa đơn còn lại (amount - amount_used)
    private Long issuedThisYear;             // Số hóa đơn phát hành trong năm (status 3,4,5)
    private Double valueThisYear;            // Giá trị hóa đơn phát hành trong năm (sum of amount)

    // Getters and Setters
    public Integer getTotalInvoices() {
        return totalInvoices;
    }

    public void setTotalInvoices(Integer totalInvoices) {
        this.totalInvoices = totalInvoices;
    }

    public Integer getUsedInvoices() {
        return usedInvoices;
    }

    public void setUsedInvoices(Integer usedInvoices) {
        this.usedInvoices = usedInvoices;
    }

    public Integer getRemainingInvoices() {
        return remainingInvoices;
    }

    public void setRemainingInvoices(Integer remainingInvoices) {
        this.remainingInvoices = remainingInvoices;
    }

    public Long getIssuedThisYear() {
        return issuedThisYear;
    }

    public void setIssuedThisYear(Long issuedThisYear) {
        this.issuedThisYear = issuedThisYear;
    }

    public Double getValueThisYear() {
        return valueThisYear;
    }

    public void setValueThisYear(Double valueThisYear) {
        this.valueThisYear = valueThisYear;
    }
}
