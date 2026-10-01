package vn.hoadon.dto.errormessage;

import java.util.ArrayList;
import java.util.List;

public class ErrorMessageUpsertRequest {
    private String formPattern;
    private Integer notificationType;
    private String taxNoticeNumber;
    private String taxNoticeDate;
    private String createPlace;
    private String noticeDate;
    private String documentType;
    private List<ErrorMessageInvoiceRequest> invoices = new ArrayList<>();

    public String getFormPattern() { return formPattern; }
    public void setFormPattern(String formPattern) { this.formPattern = formPattern; }
    public Integer getNotificationType() { return notificationType; }
    public void setNotificationType(Integer notificationType) { this.notificationType = notificationType; }
    public String getTaxNoticeNumber() { return taxNoticeNumber; }
    public void setTaxNoticeNumber(String taxNoticeNumber) { this.taxNoticeNumber = taxNoticeNumber; }
    public String getTaxNoticeDate() { return taxNoticeDate; }
    public void setTaxNoticeDate(String taxNoticeDate) { this.taxNoticeDate = taxNoticeDate; }
    public String getCreatePlace() { return createPlace; }
    public void setCreatePlace(String createPlace) { this.createPlace = createPlace; }
    public String getNoticeDate() { return noticeDate; }
    public void setNoticeDate(String noticeDate) { this.noticeDate = noticeDate; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public List<ErrorMessageInvoiceRequest> getInvoices() { return invoices; }
    public void setInvoices(List<ErrorMessageInvoiceRequest> invoices) { this.invoices = invoices != null ? invoices : new ArrayList<>(); }
}
