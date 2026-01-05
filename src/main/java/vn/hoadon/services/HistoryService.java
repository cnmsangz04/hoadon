package vn.hoadon.services;

import vn.hoadon.dto.history.HistoryDto;

import java.util.List;

public interface HistoryService {
    HistoryDto save(HistoryDto dto);

    // List histories for a specific table and company
    List<HistoryDto> listByRegisterInvoice(Long companyId, Long registerInvoiceId);

    // List histories for invoices
    List<HistoryDto> listByInvoice(Long companyId, Long invoiceId);

    // Recent notifications by company (limit N)
    List<HistoryDto> listRecentByCompany(Long companyId, int limit);
}