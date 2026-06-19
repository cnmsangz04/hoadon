package vn.hoadon.dto.invoiceimport;

import java.time.LocalDateTime;

public class InvoiceImportDTO {
    public Long id;
    public Long companyId;
    public Long userId;
    public Long sourceImportId;
    public String importType;
    public String originalFilename;
    public String storedFilename;
    public String filePath;
    public String fileUrl;
    public String status;
    public Integer totalRows;
    public Integer invoiceCount;
    public Integer itemCount;
    public Integer successCount;
    public Integer errorCount;
    public String errorMessage;
    public String importedInvoiceIds;
    public String importedItemIds;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
