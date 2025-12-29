package vn.hoadon.dto.registerinvoice;

import java.time.LocalDate;

public class RegisterInvoicePrefillDto {
    // Hình thức tờ khai (from Vue when creating)
    private Integer declarationType;
    // Hình thức tờ khai: effective_date (from Vue when creating)
    private LocalDate effectiveDate;

    // Company fields
    private String companyName;
    private String taxCode;
    private String taxAuthorityName; // companies.tax_authority_city_id -> tax_authorities.name
    private String contactAddress;   // companies.address
    private String contactEmail;     // companies.email

    // Legal representative
    private String legalFullname;    // legal_representatives.fullname
    private String legalPhone;       // legal_representatives.phone
    private String legalCitizenId;   // legal_representatives.citizen_id
    private String legalPassportNo;  // legal_representatives.passport_no
    private LocalDate legalDateOfBirth; // legal_representatives.date_of_birth
    private Integer legalGender;        // legal_representatives.gender

    // getters/setters
    public Integer getDeclarationType() { return declarationType; }
    public void setDeclarationType(Integer declarationType) { this.declarationType = declarationType; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public String getTaxAuthorityName() { return taxAuthorityName; }
    public void setTaxAuthorityName(String taxAuthorityName) { this.taxAuthorityName = taxAuthorityName; }
    public String getContactAddress() { return contactAddress; }
    public void setContactAddress(String contactAddress) { this.contactAddress = contactAddress; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getLegalFullname() { return legalFullname; }
    public void setLegalFullname(String legalFullname) { this.legalFullname = legalFullname; }
    public String getLegalPhone() { return legalPhone; }
    public void setLegalPhone(String legalPhone) { this.legalPhone = legalPhone; }
    public String getLegalCitizenId() { return legalCitizenId; }
    public void setLegalCitizenId(String legalCitizenId) { this.legalCitizenId = legalCitizenId; }
    public String getLegalPassportNo() { return legalPassportNo; }
    public void setLegalPassportNo(String legalPassportNo) { this.legalPassportNo = legalPassportNo; }
    public LocalDate getLegalDateOfBirth() { return legalDateOfBirth; }
    public void setLegalDateOfBirth(LocalDate legalDateOfBirth) { this.legalDateOfBirth = legalDateOfBirth; }
    public Integer getLegalGender() { return legalGender; }
    public void setLegalGender(Integer legalGender) { this.legalGender = legalGender; }
}
