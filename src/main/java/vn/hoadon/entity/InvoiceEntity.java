package vn.hoadon.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
public class InvoiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "form_id", nullable = false)
    private Integer formId;

    @Column(name = "invoice_number_id", nullable = false)
    private Integer invoiceNumberId;

    @Column(name = "reference_id")
    private Integer referenceId;

    @Column(name = "summary_id")
    private Integer summaryId;

    @Column(name = "id_attr")
    private String idAttr;

    @Column(name = "code")
    private String code;

    @Column(name = "code_cqt")
    private String codeCqt;

    @Column(name = "name", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String name;

    @Column(name = "no")
    private Integer no;

    @Column(name = "date_export")
    private LocalDate dateExport;

    @Column(name = "note", columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @Column(name = "date_convert")
    private LocalDateTime dateConvert;

    @Column(name = "name_convert", columnDefinition = "NVARCHAR(255)")
    private String nameConvert;

    @Column(name = "status_convert", nullable = false)
    private Short statusConvert;

    @Column(name = "date_cancel")
    private LocalDate dateCancel;

    @Column(name = "reason_cancel", columnDefinition = "NVARCHAR(MAX)")
    private String reasonCancel;

    @Column(name = "bill", columnDefinition = "NVARCHAR(MAX)")
    private String bill;

    @Column(name = "customer", columnDefinition = "NVARCHAR(MAX)")
    private String customer;

    @Column(name = "related", columnDefinition = "NVARCHAR(MAX)")
    private String related;

    @Column(name = "others", columnDefinition = "NVARCHAR(MAX)")
    private String others;

    @Column(name = "detail", columnDefinition = "NVARCHAR(MAX)")
    private String detail;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "exchange_rate", nullable = false)
    private Double exchangeRate;

    @Column(name = "send_mail_directly", nullable = false)
    private Short sendMailDirectly;

    @Column(name = "discount", nullable = false)
    private Double discount;

    @Column(name = "vat_rate", nullable = false)
    private Short vatRate;

    @Column(name = "vat_rate_other", nullable = false)
    private Integer vatRateOther;

    @Column(name = "vat_amount", nullable = false)
    private Double vatAmount;

    @Column(name = "vat_amount_discount", nullable = false)
    private Double vatAmountDiscount;

    @Column(name = "total", nullable = false)
    private Double total;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "discount_amount", nullable = false)
    private Double discountAmount;

    @Column(name = "amount_in_words", columnDefinition = "NVARCHAR(255)")
    private String amountInWords;

    @Column(name = "payment", nullable = false)
    private Short payment;

    @Column(name = "payment_type", nullable = false)
    private Short paymentType;

    @Column(name = "status", nullable = false)
    private Short status;

    @Column(name = "invoice_type", nullable = false)
    private Short invoiceType;

    @Column(name = "invoice_type_adjust", nullable = false)
    private Short invoiceTypeAdjust;

    @Column(name = "lookup_code")
    private String lookupCode;

    @Column(name = "type_api", nullable = false)
    private Short typeApi;

    @Column(name = "id_partner")
    private Integer idPartner;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getFormId() { return formId; }
    public void setFormId(Integer formId) { this.formId = formId; }

    public Integer getInvoiceNumberId() { return invoiceNumberId; }
    public void setInvoiceNumberId(Integer invoiceNumberId) { this.invoiceNumberId = invoiceNumberId; }

    public Integer getReferenceId() { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }

    public Integer getSummaryId() { return summaryId; }
    public void setSummaryId(Integer summaryId) { this.summaryId = summaryId; }

    public String getIdAttr() { return idAttr; }
    public void setIdAttr(String idAttr) { this.idAttr = idAttr; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCodeCqt() { return codeCqt; }
    public void setCodeCqt(String codeCqt) { this.codeCqt = codeCqt; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getNo() { return no; }
    public void setNo(Integer no) { this.no = no; }

    public LocalDate getDateExport() { return dateExport; }
    public void setDateExport(LocalDate dateExport) { this.dateExport = dateExport; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getDateConvert() { return dateConvert; }
    public void setDateConvert(LocalDateTime dateConvert) { this.dateConvert = dateConvert; }

    public String getNameConvert() { return nameConvert; }
    public void setNameConvert(String nameConvert) { this.nameConvert = nameConvert; }

    public Short getStatusConvert() { return statusConvert; }
    public void setStatusConvert(Short statusConvert) { this.statusConvert = statusConvert; }

    public LocalDate getDateCancel() { return dateCancel; }
    public void setDateCancel(LocalDate dateCancel) { this.dateCancel = dateCancel; }

    public String getReasonCancel() { return reasonCancel; }
    public void setReasonCancel(String reasonCancel) { this.reasonCancel = reasonCancel; }

    public String getBill() { return bill; }
    public void setBill(String bill) { this.bill = bill; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public String getRelated() { return related; }
    public void setRelated(String related) { this.related = related; }

    public String getOthers() { return others; }
    public void setOthers(String others) { this.others = others; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Double getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(Double exchangeRate) { this.exchangeRate = exchangeRate; }

    public Short getSendMailDirectly() { return sendMailDirectly; }
    public void setSendMailDirectly(Short sendMailDirectly) { this.sendMailDirectly = sendMailDirectly; }

    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }

    public Short getVatRate() { return vatRate; }
    public void setVatRate(Short vatRate) { this.vatRate = vatRate; }

    public Integer getVatRateOther() { return vatRateOther; }
    public void setVatRateOther(Integer vatRateOther) { this.vatRateOther = vatRateOther; }

    public Double getVatAmount() { return vatAmount; }
    public void setVatAmount(Double vatAmount) { this.vatAmount = vatAmount; }

    public Double getVatAmountDiscount() { return vatAmountDiscount; }
    public void setVatAmountDiscount(Double vatAmountDiscount) { this.vatAmountDiscount = vatAmountDiscount; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Double discountAmount) { this.discountAmount = discountAmount; }

    public String getAmountInWords() { return amountInWords; }
    public void setAmountInWords(String amountInWords) { this.amountInWords = amountInWords; }

    public Short getPayment() { return payment; }
    public void setPayment(Short payment) { this.payment = payment; }

    public Short getPaymentType() { return paymentType; }
    public void setPaymentType(Short paymentType) { this.paymentType = paymentType; }

    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }

    public Short getInvoiceType() { return invoiceType; }
    public void setInvoiceType(Short invoiceType) { this.invoiceType = invoiceType; }

    public Short getInvoiceTypeAdjust() { return invoiceTypeAdjust; }
    public void setInvoiceTypeAdjust(Short invoiceTypeAdjust) { this.invoiceTypeAdjust = invoiceTypeAdjust; }

    public String getLookupCode() { return lookupCode; }
    public void setLookupCode(String lookupCode) { this.lookupCode = lookupCode; }

    public Short getTypeApi() { return typeApi; }
    public void setTypeApi(Short typeApi) { this.typeApi = typeApi; }

    public Integer getIdPartner() { return idPartner; }
    public void setIdPartner(Integer idPartner) { this.idPartner = idPartner; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
