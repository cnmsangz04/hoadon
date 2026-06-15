package vn.hoadon.dto.buyinvoice;

import java.time.LocalDate;

public class BuyInvoiceHistoryFilterDTO {
    private Long companyId;
    private String source;
    private String changeType;
    private LocalDate fromDate;
    private LocalDate toDate;

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
}
