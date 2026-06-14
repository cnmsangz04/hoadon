package vn.hoadon.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "company_banks")
public class CompanyBankEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String bankName;

    @Column(name = "account_number", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String accountNumber;

    @Column(name = "bank_address", columnDefinition = "NVARCHAR(255)")
    private String bankAddress;

    @Column(name = "bank_brand", columnDefinition = "NVARCHAR(255)")
    private String bankBrand;

    // Nhiều ngân hàng thuộc về một công ty
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBankAddress() { return bankAddress; }
    public void setBankAddress(String bankAddress) { this.bankAddress = bankAddress; }

    public String getBankBrand() { return bankBrand; }
    public void setBankBrand(String bankBrand) { this.bankBrand = bankBrand; }

    public CompanyEntity getCompany() { return company; }
    public void setCompany(CompanyEntity company) { this.company = company; }
}