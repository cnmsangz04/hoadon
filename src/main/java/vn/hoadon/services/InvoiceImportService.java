package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.dto.invoiceimport.InvoiceImportDTO;
import vn.hoadon.dto.invoiceimport.InvoiceImportResultDTO;
import vn.hoadon.entity.UserEntity;

public interface InvoiceImportService {
    ResponseEntity<byte[]> template(Integer category);
    InvoiceImportResultDTO importFile(MultipartFile file, Integer category, UserEntity user);
    InvoiceImportResultDTO reimport(Long importId, Integer category, UserEntity user);
    Page<InvoiceImportDTO> list(Integer category, UserEntity user, Pageable pageable);
    default ResponseEntity<byte[]> template(Integer category, Integer stockIssueType) { return template(category); }
    default InvoiceImportResultDTO importFile(MultipartFile file, Integer category, Integer stockIssueType, UserEntity user) { return importFile(file, category, user); }
    default InvoiceImportResultDTO reimport(Long importId, Integer category, Integer stockIssueType, UserEntity user) { return reimport(importId, category, user); }
    default Page<InvoiceImportDTO> list(Integer category, Integer stockIssueType, UserEntity user, Pageable pageable) { return list(category, user, pageable); }
}
