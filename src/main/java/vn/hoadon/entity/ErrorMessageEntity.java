package vn.hoadon.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "error_messages", indexes = {
        @Index(name = "idx_error_messages_company_status", columnList = "company_id,status"),
        @Index(name = "idx_error_messages_notice_date", columnList = "notice_date"),
        @Index(name = "idx_error_messages_message_type", columnList = "message_type")
})
public class ErrorMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "id_attr", length = 64, nullable = false, unique = true)
    private String idAttr;

    @Column(name = "message_code", length = 100)
    private String messageCode;

    @Column(name = "tax_notice_number", length = 100)
    private String taxNoticeNumber;

    @Column(name = "tax_response_number", length = 100)
    private String taxResponseNumber;

    @Column(name = "form_pattern", length = 30, nullable = false)
    private String formPattern = "04/SS-HĐĐT";

    @Column(name = "message_type", length = 30, nullable = false)
    private String messageType = "TBSS";

    @Column(name = "notification_type", nullable = false)
    private Integer notificationType = 1;

    @Column(name = "tax_authority_code", length = 20)
    private String taxAuthorityCode;

    @Column(name = "tax_authority_name", columnDefinition = "NVARCHAR(255)")
    private String taxAuthorityName;

    @Column(name = "tax_code", length = 20)
    private String taxCode;

    @Column(name = "taxpayer_name", columnDefinition = "NVARCHAR(400)")
    private String taxpayerName;

    @Column(name = "create_place", columnDefinition = "NVARCHAR(255)")
    private String createPlace;

    @Column(name = "notice_date", nullable = false)
    private LocalDate noticeDate;

    @Column(name = "tax_notice_date")
    private LocalDate taxNoticeDate;

    @Column(name = "document_type", length = 30, nullable = false)
    private String documentType = "INVOICE";

    @Column(name = "signed_xml", columnDefinition = "NVARCHAR(MAX)")
    private String signedXml;

    @Column(name = "signature_info", columnDefinition = "NVARCHAR(MAX)")
    private String signatureInfo;

    @Column(name = "sign_date")
    private LocalDateTime signDate;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "response_receive_file", columnDefinition = "NVARCHAR(MAX)")
    private String responseReceiveFile;

    @Column(name = "response_accept_file", columnDefinition = "NVARCHAR(MAX)")
    private String responseAcceptFile;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonManagedReference
    @OneToMany(mappedBy = "errorMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC, id ASC")
    private List<ErrorMessageInvoiceEntity> invoices = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
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
    public String getResponseReceiveFile() { return responseReceiveFile; }
    public void setResponseReceiveFile(String responseReceiveFile) { this.responseReceiveFile = responseReceiveFile; }
    public String getResponseAcceptFile() { return responseAcceptFile; }
    public void setResponseAcceptFile(String responseAcceptFile) { this.responseAcceptFile = responseAcceptFile; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<ErrorMessageInvoiceEntity> getInvoices() { return invoices; }
    public void setInvoices(List<ErrorMessageInvoiceEntity> invoices) { this.invoices = invoices != null ? invoices : new ArrayList<>(); }
}
