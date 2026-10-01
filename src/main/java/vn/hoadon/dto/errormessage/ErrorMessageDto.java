package vn.hoadon.dto.errormessage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ErrorMessageDto {
    private Long id;
    private String idAttr;
    private String messageCode;
    private String taxNoticeNumber;
    private String taxResponseNumber;
    private String formPattern;
    private String messageType;
    private Integer notificationType;
    private String taxAuthorityCode;
    private String taxAuthorityName;
    private String taxCode;
    private String taxpayerName;
    private String createPlace;
    private LocalDate noticeDate;
    private LocalDate taxNoticeDate;
    private String documentType;
    private String signedXml;
    private String signatureInfo;
    private LocalDateTime signDate;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ErrorMessageInvoiceDto> invoices = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIdAttr() { return idAttr; }
    public void setIdAttr(String idAttr) { this.idAttr = idAttr; }
    public String getMessageCode() { return messageCode; }
    public void setMessageCode(String messageCode) { this.messageCode = messageCode; }
    public String getTaxNoticeNumber() { return taxNoticeNumber; }
    public void setTaxNoticeNumber(String taxNoticeNumber) { this.taxNoticeNumber = taxNoticeNumber; }
    public String getTaxResponseNumber() { return taxResponseNumber; }
    public void setTaxResponseNumber(String taxResponseNumber) { this.taxResponseNumber = taxResponseNumber; }
    public String getFormPattern() { return formPattern; }
    public void setFormPattern(String formPattern) { this.formPattern = formPattern; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public Integer getNotificationType() { return notificationType; }
    public void setNotificationType(Integer notificationType) { this.notificationType = notificationType; }
    public String getTaxAuthorityCode() { return taxAuthorityCode; }
    public void setTaxAuthorityCode(String taxAuthorityCode) { this.taxAuthorityCode = taxAuthorityCode; }
    public String getTaxAuthorityName() { return taxAuthorityName; }
    public void setTaxAuthorityName(String taxAuthorityName) { this.taxAuthorityName = taxAuthorityName; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public String getTaxpayerName() { return taxpayerName; }
    public void setTaxpayerName(String taxpayerName) { this.taxpayerName = taxpayerName; }
    public String getCreatePlace() { return createPlace; }
    public void setCreatePlace(String createPlace) { this.createPlace = createPlace; }
    public LocalDate getNoticeDate() { return noticeDate; }
    public void setNoticeDate(LocalDate noticeDate) { this.noticeDate = noticeDate; }
    public LocalDate getTaxNoticeDate() { return taxNoticeDate; }
    public void setTaxNoticeDate(LocalDate taxNoticeDate) { this.taxNoticeDate = taxNoticeDate; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getSignedXml() { return signedXml; }
    public void setSignedXml(String signedXml) { this.signedXml = signedXml; }
    public String getSignatureInfo() { return signatureInfo; }
    public void setSignatureInfo(String signatureInfo) { this.signatureInfo = signatureInfo; }
    public LocalDateTime getSignDate() { return signDate; }
    public void setSignDate(LocalDateTime signDate) { this.signDate = signDate; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<ErrorMessageInvoiceDto> getInvoices() { return invoices; }
    public void setInvoices(List<ErrorMessageInvoiceDto> invoices) { this.invoices = invoices != null ? invoices : new ArrayList<>(); }
}
