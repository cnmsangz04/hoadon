package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.hoadon.entity.FormInvoiceEntity;

public interface FormInvoiceRepository extends JpaRepository<FormInvoiceEntity, Long>, JpaSpecificationExecutor<FormInvoiceEntity> {
    Page<FormInvoiceEntity> findByCompanyId(Long companyId, Pageable pageable);
    Page<FormInvoiceEntity> findByCompanyIdAndSystem(Long companyId, Integer system, Pageable pageable);
    
    // Find templates by system only (for system=0 templates that are shared across all companies)
    Page<FormInvoiceEntity> findBySystem(Integer system, Pageable pageable);

    // Find templates created by a user
    Page<FormInvoiceEntity> findByUserId(Long userId, Pageable pageable);

    boolean existsByCompanyIdAndSystemAndCategoryAndSerial(Long companyId, Integer system, Integer category, String serial);
    boolean existsByCompanyIdAndSystemAndCategoryAndSerialAndIdNot(Long companyId, Integer system, Integer category, String serial, Long id);

    // Find latest active VAT form for company
    FormInvoiceEntity findTopByCompanyIdAndStatusAndCategoryOrderByUpdatedAtDesc(Long companyId, Integer status, Integer category);
}