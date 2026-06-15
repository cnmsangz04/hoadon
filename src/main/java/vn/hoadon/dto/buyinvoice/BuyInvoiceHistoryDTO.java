package vn.hoadon.dto.buyinvoice;

import java.time.LocalDateTime;

public class BuyInvoiceHistoryDTO {
    private Long id;
    private Long buyInvoiceId;
    private Long companyId;
    private String companyName;
    private String companyTaxcode;
    private String changeType;
    private String source;
    private Integer previousAmount;
    private Integer newAmount;
    private Integer amountDelta;
    private Integer previousAmountUsed;
    private Integer newAmountUsed;
    private Integer previousStatus;
    private Integer newStatus;
    private Long packagePurchaseId;
    private String packageName;
    private String paymentCode;
    private Long actorUserId;
    private String actorName;
    private String actorUsername;
    private String note;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBuyInvoiceId() { return buyInvoiceId; }
    public void setBuyInvoiceId(Long buyInvoiceId) { this.buyInvoiceId = buyInvoiceId; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyTaxcode() { return companyTaxcode; }
    public void setCompanyTaxcode(String companyTaxcode) { this.companyTaxcode = companyTaxcode; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getPreviousAmount() { return previousAmount; }
    public void setPreviousAmount(Integer previousAmount) { this.previousAmount = previousAmount; }
    public Integer getNewAmount() { return newAmount; }
    public void setNewAmount(Integer newAmount) { this.newAmount = newAmount; }
    public Integer getAmountDelta() { return amountDelta; }
    public void setAmountDelta(Integer amountDelta) { this.amountDelta = amountDelta; }
    public Integer getPreviousAmountUsed() { return previousAmountUsed; }
    public void setPreviousAmountUsed(Integer previousAmountUsed) { this.previousAmountUsed = previousAmountUsed; }
    public Integer getNewAmountUsed() { return newAmountUsed; }
    public void setNewAmountUsed(Integer newAmountUsed) { this.newAmountUsed = newAmountUsed; }
    public Integer getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(Integer previousStatus) { this.previousStatus = previousStatus; }
    public Integer getNewStatus() { return newStatus; }
    public void setNewStatus(Integer newStatus) { this.newStatus = newStatus; }
    public Long getPackagePurchaseId() { return packagePurchaseId; }
    public void setPackagePurchaseId(Long packagePurchaseId) { this.packagePurchaseId = packagePurchaseId; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public String getPaymentCode() { return paymentCode; }
    public void setPaymentCode(String paymentCode) { this.paymentCode = paymentCode; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
