package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.hoadon.entity.FormInvoiceEntity;

import java.util.Optional;

public interface FormInvoiceService {
    Page<FormInvoiceEntity> pageByCompany(Long companyId, Pageable pageable);
    Page<FormInvoiceEntity> pageByCompanySystem(Long companyId, int system, Pageable pageable);
    Page<FormInvoiceEntity> pageByCompanySystemWithFilters(Long companyId, Integer system, String q, Integer category, Integer type, Integer status, Pageable pageable);
    Page<FormInvoiceEntity> pageBySystem(int system, Pageable pageable); // For system templates (system=0) shared across all companies
    Page<FormInvoiceEntity> pageAll(Pageable pageable);

    Page<FormInvoiceEntity> pageByUser(Long userId, Pageable pageable);
    Optional<FormInvoiceEntity> findById(Long id);
    FormInvoiceEntity create(FormInvoiceEntity e);
    Optional<FormInvoiceEntity> update(Long id, FormInvoiceEntity patch);
    void delete(Long id);
}