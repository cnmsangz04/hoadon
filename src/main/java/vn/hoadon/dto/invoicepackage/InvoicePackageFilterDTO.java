package vn.hoadon.dto.invoicepackage;

public class InvoicePackageFilterDTO {
    private String keyword;
    private Integer status;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
