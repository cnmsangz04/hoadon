package vn.hoadon.services;

import vn.hoadon.dto.SignatureVatDTO;

import java.util.List;

public interface SignatureVatService {
    SignatureVatDTO create(SignatureVatDTO dto);
    SignatureVatDTO getLatestByInvoiceId(Integer invoiceId);
    List<SignatureVatDTO> listByInvoiceId(Integer invoiceId);
}
