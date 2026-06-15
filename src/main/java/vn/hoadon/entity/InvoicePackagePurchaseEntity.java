package vn.hoadon.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_package_purchases")
public class InvoicePackagePurchaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "package_name", columnDefinition = "NVARCHAR(255)")
    private String packageName;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "company_name", columnDefinition = "NVARCHAR(255)")
    private String companyName;

    @Column(name = "company_taxcode", columnDefinition = "NVARCHAR(255)")
    private String companyTaxcode;

    @Column(name = "buyer_name", columnDefinition = "NVARCHAR(255)")
    private String buyerName;

    @Column(name = "buyer_email", columnDefinition = "NVARCHAR(255)")
    private String buyerEmail;

    @Column(name = "buyer_phone", columnDefinition = "NVARCHAR(50)")
    private String buyerPhone;

    @Column(name = "invoice_quantity", nullable = false)
    private Integer invoiceQuantity;

    @Column(name = "unit_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "payment_method", columnDefinition = "NVARCHAR(50)")
    private String paymentMethod;

    @Column(name = "payment_status", columnDefinition = "NVARCHAR(50)")
    private String paymentStatus;

    @Column(name = "payment_code", columnDefinition = "NVARCHAR(100)")
    private String paymentCode;

    @Column(name = "buy_invoice_id")
    private Long buyInvoiceId;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCompanyTaxcode() { return companyTaxcode; }
    public void setCompanyTaxcode(String companyTaxcode) { this.companyTaxcode = companyTaxcode; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }

    public String getBuyerPhone() { return buyerPhone; }
    public void setBuyerPhone(String buyerPhone) { this.buyerPhone = buyerPhone; }

    public Integer getInvoiceQuantity() { return invoiceQuantity; }
    public void setInvoiceQuantity(Integer invoiceQuantity) { this.invoiceQuantity = invoiceQuantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentCode() { return paymentCode; }
    public void setPaymentCode(String paymentCode) { this.paymentCode = paymentCode; }

    public Long getBuyInvoiceId() { return buyInvoiceId; }
    public void setBuyInvoiceId(Long buyInvoiceId) { this.buyInvoiceId = buyInvoiceId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
