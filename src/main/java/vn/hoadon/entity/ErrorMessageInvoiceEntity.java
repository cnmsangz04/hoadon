package vn.hoadon.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "error_message_invoices", indexes = {
        @Index(name = "idx_error_message_invoices_message", columnList = "error_message_id"),
        @Index(name = "idx_error_message_invoices_invoice", columnList = "invoice_id")
})
public class ErrorMessageInvoiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "error_message_id", nullable = false)
    private ErrorMessageEntity errorMessage;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "tax_code", length = 40)
    private String taxCode;

    @Column(name = "form_symbol", length = 20)
    private String formSymbol;

    @Column(name = "serial", length = 30)
    private String serial;

    @Column(name = "invoice_no", length = 20)
    private String invoiceNo;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "invoice_type", nullable = false)
    private Integer invoiceType = 1;

    @Column(name = "error_type", nullable = false)
    private Integer errorType = 0;

    @Column(name = "reason", columnDefinition = "NVARCHAR(500)")
    private String reason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ErrorMessageEntity getErrorMessage() { return errorMessage; }
    public void setErrorMessage(ErrorMessageEntity errorMessage) { this.errorMessage = errorMessage; }
    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public Integer getLineNo() { return lineNo; }
    public void setLineNo(Integer lineNo) { this.lineNo = lineNo; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public String getFormSymbol() { return formSymbol; }
    public void setFormSymbol(String formSymbol) { this.formSymbol = formSymbol; }
    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public Integer getInvoiceType() { return invoiceType; }
    public void setInvoiceType(Integer invoiceType) { this.invoiceType = invoiceType; }
    public Integer getErrorType() { return errorType; }
    public void setErrorType(Integer errorType) { this.errorType = errorType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
