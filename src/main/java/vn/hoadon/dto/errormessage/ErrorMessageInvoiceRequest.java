package vn.hoadon.dto.errormessage;

public class ErrorMessageInvoiceRequest {
    private Long id;
    private Long invoiceId;
    private String taxCode;
    private String formSymbol;
    private String serial;
    private String invoiceNo;
    private String invoiceDate;
    private Integer invoiceType;
    private Integer errorType;
    private String reason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public String getFormSymbol() { return formSymbol; }
    public void setFormSymbol(String formSymbol) { this.formSymbol = formSymbol; }
    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public String getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(String invoiceDate) { this.invoiceDate = invoiceDate; }
    public Integer getInvoiceType() { return invoiceType; }
    public void setInvoiceType(Integer invoiceType) { this.invoiceType = invoiceType; }
    public Integer getErrorType() { return errorType; }
    public void setErrorType(Integer errorType) { this.errorType = errorType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
