package vn.hoadon.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "buy_invoice_histories")
public class BuyInvoiceHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buy_invoice_id")
    private Long buyInvoiceId;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_name", columnDefinition = "NVARCHAR(255)")
    private String companyName;

    @Column(name = "company_taxcode", columnDefinition = "NVARCHAR(64)")
    private String companyTaxcode;

    @Column(name = "change_type", length = 40)
    private String changeType;

    @Column(name = "source", length = 30)
    private String source;

    @Column(name = "previous_amount")
    private Integer previousAmount;

    @Column(name = "new_amount")
    private Integer newAmount;

    @Column(name = "amount_delta")
    private Integer amountDelta;

    @Column(name = "previous_amount_used")
    private Integer previousAmountUsed;

    @Column(name = "new_amount_used")
    private Integer newAmountUsed;

    @Column(name = "previous_status")
    private Integer previousStatus;

    @Column(name = "new_status")
    private Integer newStatus;

    @Column(name = "package_purchase_id")
    private Long packagePurchaseId;

    @Column(name = "package_name", columnDefinition = "NVARCHAR(255)")
    private String packageName;

    @Column(name = "payment_code", columnDefinition = "NVARCHAR(100)")
    private String paymentCode;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_name", columnDefinition = "NVARCHAR(255)")
    private String actorName;

    @Column(name = "actor_username", columnDefinition = "NVARCHAR(255)")
    private String actorUsername;

    @Column(name = "note", columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

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
