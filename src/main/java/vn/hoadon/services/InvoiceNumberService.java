package vn.hoadon.services;

import vn.hoadon.entity.InvoiceNumberEntity;

import java.util.List;
import java.util.Optional;

public interface InvoiceNumberService {
    InvoiceNumberEntity create(InvoiceNumberEntity entity);
    Optional<InvoiceNumberEntity> findLatestByCompanyAndForm(Long companyId, Long formId);
    List<InvoiceNumberEntity> findActiveByCompany(Long companyId);
    InvoiceNumberEntity update(InvoiceNumberEntity entity);
}