package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.hoadon.entity.FormInvoiceEntity;

import java.util.Optional;

public interface FormInvoiceService {
    Page<FormInvoiceEntity> pageByCompany(Long companyId, Pageable pageable);
    Page<FormInvoiceEntity> pageByCompanySystem(Long companyId, int system, Pageable pageable);
    Page<FormInvoiceEntity> pageAll(Pageable pageable);
    Optional<FormInvoiceEntity> findById(Long id);
    FormInvoiceEntity create(FormInvoiceEntity e);
    Optional<FormInvoiceEntity> update(Long id, FormInvoiceEntity patch);
    void delete(Long id);
}