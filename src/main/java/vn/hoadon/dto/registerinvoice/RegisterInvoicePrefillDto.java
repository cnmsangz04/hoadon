package vn.hoadon.dto.registerinvoice;

import java.time.LocalDate;

public class RegisterInvoicePrefillDto {
    // Hình thức tờ khai (from Vue when creating)
    private Integer declarationType;
    // Hình thức tờ khai: effective_date (from Vue when creating)
    private LocalDate effectiveDate;

    // Trường thông tin công ty
    private String companyName;
    private String taxCode;
    private String taxAuthorityName; // tên cơ quan thuế từ companies.tax_authority_city_id -> tax_authorities.name
    private String contactAddress;   // địa chỉ từ companies.address
    private String contactEmail;     // email từ companies.email

    // Người đại diện pháp luật
    private String legalFullname;    // họ tên từ legal_representatives.fullname
    private String legalPhone;       // số điện thoại từ legal_representatives.phone
    private String legalCitizenId;   // CCCD/CMND từ legal_representatives.citizen_id
    private String legalPassportNo;  // số hộ chiếu từ legal_representatives.passport_no
    private LocalDate legalDateOfBirth; // ngày sinh từ legal_representatives.date_of_birth
    private Integer legalGender;        // giới tính từ legal_representatives.gender

    // hàm getter/setter
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
