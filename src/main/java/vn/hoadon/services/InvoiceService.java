package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.hoadon.dto.InvoiceDTO;
import vn.hoadon.entity.InvoiceEntity;

public interface InvoiceService {
    Page<InvoiceDTO> search(String q, Short status, Pageable pageable);
    // Create a new invoice from a payload and return the persisted entity
    InvoiceEntity create(InvoicePayload payload, Long companyId, Long userId);
    // Update an existing invoice by id
    InvoiceEntity update(Long id, InvoicePayload payload, Long companyId, Long userId);

    // Minimal payload contract matching Vue form
    class InvoicePayload {
        public Long formId;
        public Integer no;
        public java.time.LocalDate dateExport;
        public Short paymentType;
        public Short status; // optional, default 0
        public String orderCode;
        public Object customer; // will be serialized to JSON text
        public Object detail;   // will be serialized to JSON text
        public Double total;
        public Double vatAmount;
        public Double amount;
        public String amountInWords;
        public String currency; // default 'VND'
        public Double exchangeRate; // default 0
        public Short vatRate; // summary if any; optional
        public Integer vatRateOther; // optional
    }
}