package vn.hoadon.dto.invoiceimport;

public class InvoiceImportResultDTO {
    public Long id;
    public String status;
    public Integer totalRows;
    public Integer invoiceCount;
    public Integer successCount;
    public Integer errorCount;
    public String errorMessage;
    public String fileUrl;
    public String importedInvoiceIds;
}
