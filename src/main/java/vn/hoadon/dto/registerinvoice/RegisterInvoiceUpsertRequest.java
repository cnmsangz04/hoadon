package vn.hoadon.dto.registerinvoice;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterInvoiceUpsertRequest {
    // Thông tin tờ khai
    @JsonProperty("declarationType")
    @JsonAlias({"declaration_type"})
    private Integer declarationType;

    @JsonProperty("formPattern")
    @JsonAlias({"form_pattern"})
    private String formPattern;

    @JsonProperty("declarationDate")
    @JsonAlias({"declaration_date"})
    private String declarationDate; // ISO-8601 string (yyyy-MM-dd)

    // Doanh nghiệp
    @JsonProperty("companyName")
    @JsonAlias({"company_name"})
    private String companyName;

    @JsonProperty("taxCode")
    @JsonAlias({"tax_code"})
    private String taxCode;

    @JsonProperty("taxAuthorityName")
    @JsonAlias({"tax_authority_name"})
    private String taxAuthorityName;

    @JsonProperty("taxAuthorityCode")
    @JsonAlias({"tax_authority_code"})
    private String taxAuthorityCode;

    // Người liên hệ (đại diện pháp luật)
    @JsonProperty("contactName")
    @JsonAlias({"contact_name"})
    private String contactName;

    @JsonProperty("contactPhone")
    @JsonAlias({"contact_phone"})
    private String contactPhone;

    @JsonProperty("contactEmail")
    @JsonAlias({"contact_email"})
    private String contactEmail;

    @JsonProperty("contactAddress")
    @JsonAlias({"contact_address"})
    private String contactAddress;

    // Địa danh, hiệu lực
    @JsonProperty("createPlace")
    @JsonAlias({"create_place"})
    private String createPlace; // province id or name

    @JsonProperty("effectiveDate")
    @JsonAlias({"effective_date"})
    private String effectiveDate; // ISO-8601 string

    // Nghiệp vụ: có thể đến dưới dạng mảng/đối tượng hoặc chuỗi JSON
    @JsonProperty("invoiceForms")
    @JsonAlias({"invoice_forms"})
    private Object invoiceForms;

    @JsonProperty("invoiceTypes")
    @JsonAlias({"invoice_types"})
    private Object invoiceTypes;

    @JsonProperty("transferMethods")
    @JsonAlias({"transfer_methods"})
    private Object transferMethods;

    @JsonProperty("sendMethods")
    @JsonAlias({"send_methods"})
    private Object sendMethods; // Map<String, List<String>> hoặc chuỗi JSON

    @JsonProperty("digitalCertificates")
    @JsonAlias({"digital_certificates"})
    private Object digitalCertificates; // List<DigitalCertificate> hoặc chuỗi JSON

    @JsonProperty("solutionProviders")
    @JsonAlias({"solution_providers"})
    private Object solutionProviders;

    @JsonProperty("transmitProviders")
    @JsonAlias({"transmit_providers"})
    private Object transmitProviders;

    @JsonProperty("status")
    private Integer status;

    // getters & setters
    public Integer getDeclarationType() { return declarationType; }
    public void setDeclarationType(Integer declarationType) { this.declarationType = declarationType; }
    public String getFormPattern() { return formPattern; }
    public void setFormPattern(String formPattern) { this.formPattern = formPattern; }
    public String getDeclarationDate() { return declarationDate; }
    public void setDeclarationDate(String declarationDate) { this.declarationDate = declarationDate; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public String getTaxAuthorityName() { return taxAuthorityName; }
    public void setTaxAuthorityName(String taxAuthorityName) { this.taxAuthorityName = taxAuthorityName; }
    public String getTaxAuthorityCode() { return taxAuthorityCode; }
    public void setTaxAuthorityCode(String taxAuthorityCode) { this.taxAuthorityCode = taxAuthorityCode; }
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
    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }
    public Object getInvoiceForms() { return invoiceForms; }
    public void setInvoiceForms(Object invoiceForms) { this.invoiceForms = invoiceForms; }
    public Object getInvoiceTypes() { return invoiceTypes; }
    public void setInvoiceTypes(Object invoiceTypes) { this.invoiceTypes = invoiceTypes; }
    public Object getTransferMethods() { return transferMethods; }
    public void setTransferMethods(Object transferMethods) { this.transferMethods = transferMethods; }
    public Object getSendMethods() { return sendMethods; }
    public void setSendMethods(Object sendMethods) { this.sendMethods = sendMethods; }
    public Object getDigitalCertificates() { return digitalCertificates; }
    public void setDigitalCertificates(Object digitalCertificates) { this.digitalCertificates = digitalCertificates; }
    public Object getSolutionProviders() { return solutionProviders; }
    public void setSolutionProviders(Object solutionProviders) { this.solutionProviders = solutionProviders; }
    public Object getTransmitProviders() { return transmitProviders; }
    public void setTransmitProviders(Object transmitProviders) { this.transmitProviders = transmitProviders; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DigitalCertificate {
        @JsonProperty("orgName")
        private String orgName;
        @JsonProperty("serialNo")
        private String serialNo;
        @JsonProperty("signFromDate")
        private String signFromDate; // ISO string
        @JsonProperty("signToDate")
        private String signToDate;   // ISO string
        @JsonProperty("sigRegMethod")
        private String sigRegMethod; // ADD/EXTEND/STOP
        public String getOrgName() { return orgName; }
        public void setOrgName(String orgName) { this.orgName = orgName; }
        public String getSerialNo() { return serialNo; }
        public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
        public String getSignFromDate() { return signFromDate; }
        public void setSignFromDate(String signFromDate) { this.signFromDate = signFromDate; }
        public String getSignToDate() { return signToDate; }
        public void setSignToDate(String signToDate) { this.signToDate = signToDate; }
        public String getSigRegMethod() { return sigRegMethod; }
        public void setSigRegMethod(String sigRegMethod) { this.sigRegMethod = sigRegMethod; }
    }
}