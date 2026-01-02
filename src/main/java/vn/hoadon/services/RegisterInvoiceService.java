package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    Page<vn.hoadon.entity.RegisterInvoiceEntity> pageByCompany(Long companyId, Pageable pageable);
    Page<vn.hoadon.entity.RegisterInvoiceEntity> pageByCompanyAndStatus(Long companyId, Integer status, Pageable pageable);
    Page<vn.hoadon.entity.RegisterInvoiceEntity> pageAll(Pageable pageable);
    /** Build unsigned XML from entity */
    String buildUnsignedXml(RegisterInvoiceEntity entity);
    /** Get XML for download: if status == 0, return unsigned; else return signed_xml */
    Optional<String> getXmlForDownload(Long id);
    /** Delete a register invoice by id */
    void delete(Long id);
}