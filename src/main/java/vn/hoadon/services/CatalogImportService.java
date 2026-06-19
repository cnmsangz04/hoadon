package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.dto.invoiceimport.InvoiceImportDTO;
import vn.hoadon.dto.invoiceimport.InvoiceImportResultDTO;
import vn.hoadon.entity.UserEntity;

public interface CatalogImportService {
    ResponseEntity<byte[]> template(String type);
    InvoiceImportResultDTO importFile(String type, MultipartFile file, UserEntity user);
    InvoiceImportResultDTO reimport(String type, Long importId, UserEntity user);
    Page<InvoiceImportDTO> list(String type, UserEntity user, Pageable pageable);
}
