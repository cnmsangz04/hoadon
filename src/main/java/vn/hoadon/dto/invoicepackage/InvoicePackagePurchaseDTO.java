package vn.hoadon.dto.invoicepackage;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoicePackagePurchaseDTO {
    private Long id;
    private Long packageId;
    private String packageName;
    private Long companyId;
    private String companyName;
    private String companyTaxcode;
    private String buyerName;
    private String buyerEmail;
    private String buyerPhone;
    private Integer invoiceQuantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentCode;
    private Long buyInvoiceId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private Integer companyStatus;
    private Integer totalInvoices;
    private Integer usedInvoices;
    private Integer remainingInvoices;

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

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getCompanyStatus() { return companyStatus; }
    public void setCompanyStatus(Integer companyStatus) { this.companyStatus = companyStatus; }

    public Integer getTotalInvoices() { return totalInvoices; }
    public void setTotalInvoices(Integer totalInvoices) { this.totalInvoices = totalInvoices; }

    public Integer getUsedInvoices() { return usedInvoices; }
    public void setUsedInvoices(Integer usedInvoices) { this.usedInvoices = usedInvoices; }

    public Integer getRemainingInvoices() { return remainingInvoices; }
    public void setRemainingInvoices(Integer remainingInvoices) { this.remainingInvoices = remainingInvoices; }
}
