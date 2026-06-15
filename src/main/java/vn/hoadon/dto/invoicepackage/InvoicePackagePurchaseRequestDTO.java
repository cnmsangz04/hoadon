package vn.hoadon.dto.invoicepackage;

public class InvoicePackagePurchaseRequestDTO {
    private Long packageId;
    private String paymentMethod;

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
