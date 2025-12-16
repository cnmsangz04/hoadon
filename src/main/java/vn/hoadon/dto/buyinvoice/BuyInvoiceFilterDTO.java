package vn.hoadon.dto.buyinvoice;

public class BuyInvoiceFilterDTO {
    private Long companyId;
    private Integer status;
    private String keyword;

    // getter / setter
	public Long getCompanyId() {
		return companyId;
	}
	public void setCompanyId(Long companyId) {
		this.companyId = companyId;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
}