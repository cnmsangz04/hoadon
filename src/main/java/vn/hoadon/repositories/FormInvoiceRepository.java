package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.hoadon.entity.FormInvoiceEntity;

public interface FormInvoiceRepository extends JpaRepository<FormInvoiceEntity, Long>, JpaSpecificationExecutor<FormInvoiceEntity> {
    Page<FormInvoiceEntity> findByCompanyId(Long companyId, Pageable pageable);
    Page<FormInvoiceEntity> findByCompanyIdAndSystem(Long companyId, Integer system, Pageable pageable);

    boolean existsByCompanyIdAndSystemAndCategoryAndSerial(Long companyId, Integer system, Integer category, String serial);
    boolean existsByCompanyIdAndSystemAndCategoryAndSerialAndIdNot(Long companyId, Integer system, Integer category, String serial, Long id);
}