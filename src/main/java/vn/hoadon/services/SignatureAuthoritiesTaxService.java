package vn.hoadon.services;

import vn.hoadon.dto.SignatureAuthoritiesTaxDTO;

import java.util.List;

public interface SignatureAuthoritiesTaxService {
    SignatureAuthoritiesTaxDTO create(SignatureAuthoritiesTaxDTO dto);
    SignatureAuthoritiesTaxDTO getLatestByInvoiceId(Integer invoiceId);
    List<SignatureAuthoritiesTaxDTO> listByInvoiceId(Integer invoiceId);
    SignatureAuthoritiesTaxDTO getLatestByInvoiceIdAndCompanyId(Integer invoiceId, Integer companyId);
    List<SignatureAuthoritiesTaxDTO> listByInvoiceIdAndCompanyId(Integer invoiceId, Integer companyId);
}
