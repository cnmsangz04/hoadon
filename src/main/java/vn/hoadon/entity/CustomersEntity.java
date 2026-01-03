package vn.hoadon.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class CustomersEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "company_id", nullable = false)
	private Long companyId;
	
	@Column(name = "code", columnDefinition = "NVARCHAR(255)", nullable = false)
	private String code;
	
	@Column(name = "tax_code", columnDefinition = "NVARCHAR(255)")
	private String taxCode;
	
	@Column(name = "company_name", columnDefinition = "NVARCHAR(255)")
	private String companyName;
	
	@Column(name = "buyer_name", columnDefinition = "NVARCHAR(255)")
	private String buyerName;
	
	@Column(name = "address", columnDefinition = "NVARCHAR(255)")
	private String address;
	
	@Column(name = "phone", columnDefinition = "NVARCHAR(255)")
	private String phone;
	
	@Column(name = "fax", columnDefinition = "NVARCHAR(255)")
	private String fax;
	
	@Column(name = "email", columnDefinition = "NVARCHAR(255)")
	private String email;
	
	@Column(name = "bank_account_number", columnDefinition = "NVARCHAR(255)")
	private String bankAccountNumber;
	
	@Column(name = "bank_name", columnDefinition = "NVARCHAR(255)")
	private String bankName;
	
	@Column(name = "description", columnDefinition = "NVARCHAR(255)")
	private String description;
	
	@Column(name = "status", columnDefinition = "TINYINT", nullable = false)
	private Integer status = 1;
	
	
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	
	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCompanyId() {
		return companyId;
	}

	public void setCompanyId(Long companyId) {
		this.companyId = companyId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getTaxCode() {
		return taxCode;
	}

	public void setTaxCode(String taxCode) {
		this.taxCode = taxCode;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getBuyerName() {
		return buyerName;
	}

	public void setBuyerName(String buyerName) {
		this.buyerName = buyerName;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getFax() {
		return fax;
	}

	public void setFax(String fax) {
		this.fax = fax;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getBankAccountNumber() {
		return bankAccountNumber;
	}

	public void setBankAccountNumber(String bankAccountNumber) {
		this.bankAccountNumber = bankAccountNumber;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
	
}
