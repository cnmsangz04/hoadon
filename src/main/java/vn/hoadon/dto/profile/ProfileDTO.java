package vn.hoadon.dto.profile;

import java.time.LocalDate;

public class ProfileDTO {
    // company
    public Long companyId;
    public String taxCode;
    public String companyName;
    public String companyBusiness;
    public String companyAddress;
    public String companyLogo;
    public String companyFavicon;

    // representative
    public String representName;
    public Integer representGender;
    public LocalDate representDateBirth;
    public String representCitizenIdent;
    public String representPassPort;
    public String representPhone;
    public String representMail;

    // invoice
    public String invoiceEmail;
    public String invoicePhone;
    public String invoiceFax;
    public String invoiceWebsite;

    // contact for tax
    public String contactName;
    public String contactMail;
    public String contactPhone;
    public String contactAddress;

    // bank
    public String bankName; // display name
    public String bankAbbreviation; // code used by select
    public String bankNo;
    public String bankAddress;
    public String bankBrand;
    public String bankNameCustom;
    public String bankSeparatorCharacter;

    // tax authority
    public Integer taxAuthorityCity;
    public Integer taxAuthorityName;

    // display blade values
    public String bladeTaxAuthorityCity;
    public String bladeTaxAuthorityName;
}