package vn.hoadon.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "register_invoices")
public class RegisterInvoiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan hệ hệ thống
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Thông tin tờ khai
    @Column(name = "declaration_code", length = 100)
    private String declarationCode;

    @Column(name = "declaration_type", nullable = false)
    private Integer declarationType = 1; // 1: ĐK mới, 2: Thay đổi

    @Column(name = "form_pattern", length = 20, nullable = false)
    private String formPattern;

    @Column(name = "declaration_date", nullable = false)
    private LocalDate declarationDate;

    // Doanh nghiệp
    @Column(name = "company_name", length = 255, nullable = false)
    private String companyName;

    @Column(name = "tax_code", length = 16, nullable = false)
    private String taxCode;

    // Cơ quan thuế
    @Column(name = "tax_authority_code", length = 10, nullable = false)
    private String taxAuthorityCode;

    @Column(name = "tax_authority_name", length = 255, nullable = false)
    private String taxAuthorityName;

    // Người liên hệ
    @Column(name = "contact_name", length = 255, nullable = false)
    private String contactName;

    @Column(name = "contact_phone", length = 32, nullable = false)
    private String contactPhone;

    @Column(name = "contact_email", length = 255, nullable = false)
    private String contactEmail;

    @Column(name = "contact_address", length = 500, nullable = false)
    private String contactAddress;

    // Địa danh, hiệu lực
    @Column(name = "create_place", length = 255, nullable = false)
    private String createPlace;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    // Dữ liệu nghiệp vụ (JSON as NVARCHAR(MAX))
    @Column(name = "invoice_forms", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String invoiceForms;

    @Column(name = "invoice_types", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String invoiceTypes;

    @Column(name = "send_methods", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String sendMethods;

    @Column(name = "transfer_methods", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String transferMethods;

    @Column(name = "digital_certificates", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String digitalCertificates;

    @Column(name = "solution_providers", columnDefinition = "NVARCHAR(MAX)")
    private String solutionProviders;

    @Column(name = "transmit_providers", columnDefinition = "NVARCHAR(MAX)")
    private String transmitProviders;

    // XML & chữ ký
    @Column(name = "signed_xml", columnDefinition = "NVARCHAR(MAX)")
    private String signedXml;

    @Column(name = "signature_info", columnDefinition = "NVARCHAR(MAX)")
    private String signatureInfo;

    @Column(name = "sign_date")
    private LocalDateTime signDate;

    // Trạng thái xử lý
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "response_receive_file", length = 255)
    private String responseReceiveFile;

    @Column(name = "response_accept_file", length = 255)
    private String responseAcceptFile;

    // Audit
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and setters omitted for brevity
    // ...existing code...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDeclarationCode() { return declarationCode; }
    public void setDeclarationCode(String declarationCode) { this.declarationCode = declarationCode; }
    public Integer getDeclarationType() { return declarationType; }
    public void setDeclarationType(Integer declarationType) { this.declarationType = declarationType; }
    public String getFormPattern() { return formPattern; }
    public void setFormPattern(String formPattern) { this.formPattern = formPattern; }
    public LocalDate getDeclarationDate() { return declarationDate; }
    public void setDeclarationDate(LocalDate declarationDate) { this.declarationDate = declarationDate; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public String getTaxAuthorityCode() { return taxAuthorityCode; }
    public void setTaxAuthorityCode(String taxAuthorityCode) { this.taxAuthorityCode = taxAuthorityCode; }
    public String getTaxAuthorityName() { return taxAuthorityName; }
    public void setTaxAuthorityName(String taxAuthorityName) { this.taxAuthorityName = taxAuthorityName; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactAddress() { return contactAddress; }
    public void setContactAddress(String contactAddress) { this.contactAddress = contactAddress; }
    public String getCreatePlace() { return createPlace; }
    public void setCreatePlace(String createPlace) { this.createPlace = createPlace; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getInvoiceForms() { return invoiceForms; }
    public void setInvoiceForms(String invoiceForms) { this.invoiceForms = invoiceForms; }
    public String getInvoiceTypes() { return invoiceTypes; }
    public void setInvoiceTypes(String invoiceTypes) { this.invoiceTypes = invoiceTypes; }
    public String getSendMethods() { return sendMethods; }
    public void setSendMethods(String sendMethods) { this.sendMethods = sendMethods; }
    public String getTransferMethods() { return transferMethods; }
    public void setTransferMethods(String transferMethods) { this.transferMethods = transferMethods; }
    public String getDigitalCertificates() { return digitalCertificates; }
    public void setDigitalCertificates(String digitalCertificates) { this.digitalCertificates = digitalCertificates; }
    public String getSolutionProviders() { return solutionProviders; }
    public void setSolutionProviders(String solutionProviders) { this.solutionProviders = solutionProviders; }
    public String getTransmitProviders() { return transmitProviders; }
    public void setTransmitProviders(String transmitProviders) { this.transmitProviders = transmitProviders; }
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
}
