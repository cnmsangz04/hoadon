package vn.hoadon.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_imports")
public class InvoiceImportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "source_import_id")
    private Long sourceImportId;

    @Column(name = "import_type", length = 30)
    private String importType;

    @Column(name = "original_filename", columnDefinition = "NVARCHAR(255)")
    private String originalFilename;

    @Column(name = "stored_filename", columnDefinition = "NVARCHAR(255)")
    private String storedFilename;

    @Column(name = "file_path", columnDefinition = "NVARCHAR(500)")
    private String filePath;

    @Column(name = "file_url", columnDefinition = "NVARCHAR(500)")
    private String fileUrl;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "invoice_count")
    private Integer invoiceCount;

    @Column(name = "item_count")
    private Integer itemCount;

    @Column(name = "success_count")
    private Integer successCount;

    @Column(name = "error_count")
    private Integer errorCount;

    @Column(name = "error_message", columnDefinition = "NVARCHAR(MAX)")
    private String errorMessage;

    @Column(name = "imported_invoice_ids", columnDefinition = "NVARCHAR(MAX)")
    private String importedInvoiceIds;

    @Column(name = "imported_item_ids", columnDefinition = "NVARCHAR(MAX)")
    private String importedItemIds;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSourceImportId() { return sourceImportId; }
    public void setSourceImportId(Long sourceImportId) { this.sourceImportId = sourceImportId; }
    public String getImportType() { return importType; }
    public void setImportType(String importType) { this.importType = importType; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }
    public Integer getInvoiceCount() { return invoiceCount; }
    public void setInvoiceCount(Integer invoiceCount) { this.invoiceCount = invoiceCount; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getImportedInvoiceIds() { return importedInvoiceIds; }
    public void setImportedInvoiceIds(String importedInvoiceIds) { this.importedInvoiceIds = importedInvoiceIds; }
    public String getImportedItemIds() { return importedItemIds; }
    public void setImportedItemIds(String importedItemIds) { this.importedItemIds = importedItemIds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
