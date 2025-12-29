package vn.hoadon.services;

import vn.hoadon.dto.registerinvoice.RegisterInvoicePrefillDto;
import vn.hoadon.entity.RegisterInvoiceEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RegisterInvoiceService {
    Optional<RegisterInvoiceEntity> findById(Long id);
    List<RegisterInvoiceEntity> findByCompany(Long companyId);
    List<RegisterInvoiceEntity> findByUser(Long userId);
    RegisterInvoiceEntity create(RegisterInvoiceEntity entity);
    Optional<RegisterInvoiceEntity> update(Long id, RegisterInvoiceEntity patch);
    Optional<RegisterInvoiceEntity> attachSignedXml(Long id, String signedXml, String signatureInfo);
    Optional<RegisterInvoicePrefillDto> prefill(Long companyId, Integer declarationType, LocalDate effectiveDate);
}
