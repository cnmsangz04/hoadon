package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.hoadon.dto.InvoiceDTO;

public interface InvoiceService {
    Page<InvoiceDTO> search(String q, Short status, Pageable pageable);
}
