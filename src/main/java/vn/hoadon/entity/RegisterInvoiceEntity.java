package vn.hoadon.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import vn.hoadon.entity.converters.JsonListConverter;

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

    // Địa danh, hiệu lực
    @Column(name = "create_place", length = 255, nullable = false)
    private String createPlace;

    @Column(name = "effective_date", nullable = true)
    private LocalDateTime effectiveDate;

    // Dữ liệu nghiệp vụ (JSON as NVARCHAR(MAX))
    @Convert(converter = JsonListConverter.class)
    @Column(name = "invoice_forms", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private List<String> invoiceForms;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "invoice_types", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private List<String> invoiceTypes;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "send_methods", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private List<String> sendMethods;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "transfer_methods", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private List<String> transferMethods;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "digital_certificates", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private List<String> digitalCertificates;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "solution_providers", columnDefinition = "NVARCHAR(MAX)")
    private List<String> solutionProviders;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "transmit_providers", columnDefinition = "NVARCHAR(MAX)")
    private List<String> transmitProviders;

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

    // Getters and setters
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
    public String getCreatePlace() { return createPlace; }
    public void setCreatePlace(String createPlace) { this.createPlace = createPlace; }
    public LocalDateTime getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDateTime effectiveDate) { this.effectiveDate = effectiveDate; }
    public List<String> getInvoiceForms() { return invoiceForms; }
    public void setInvoiceForms(List<String> invoiceForms) { this.invoiceForms = invoiceForms; }
    public List<String> getInvoiceTypes() { return invoiceTypes; }
    public void setInvoiceTypes(List<String> invoiceTypes) { this.invoiceTypes = invoiceTypes; }
    public List<String> getSendMethods() { return sendMethods; }
    public void setSendMethods(List<String> sendMethods) { this.sendMethods = sendMethods; }
    public List<String> getTransferMethods() { return transferMethods; }
    public void setTransferMethods(List<String> transferMethods) { this.transferMethods = transferMethods; }
    public List<String> getDigitalCertificates() { return digitalCertificates; }
    public void setDigitalCertificates(List<String> digitalCertificates) { this.digitalCertificates = digitalCertificates; }
    public List<String> getSolutionProviders() { return solutionProviders; }
    public void setSolutionProviders(List<String> solutionProviders) { this.solutionProviders = solutionProviders; }
    public List<String> getTransmitProviders() { return transmitProviders; }
    public void setTransmitProviders(List<String> transmitProviders) { this.transmitProviders = transmitProviders; }
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