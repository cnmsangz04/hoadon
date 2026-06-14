package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.hoadon.entity.FormInvoiceEntity;

public interface FormInvoiceRepository extends JpaRepository<FormInvoiceEntity, Long>, JpaSpecificationExecutor<FormInvoiceEntity> {
    Page<FormInvoiceEntity> findByCompanyId(Long companyId, Pageable pageable);
    Page<FormInvoiceEntity> findByCompanyIdAndSystem(Long companyId, Integer system, Pageable pageable);
    
    // Tìm template chỉ theo system (system = 0 là template dùng chung cho mọi công ty)
    Page<FormInvoiceEntity> findBySystem(Integer system, Pageable pageable);

    // Tìm template do người dùng tạo
    Page<FormInvoiceEntity> findByUserId(Long userId, Pageable pageable);

    boolean existsByCompanyIdAndSystemAndCategoryAndSerial(Long companyId, Integer system, Integer category, String serial);
    boolean existsByCompanyIdAndSystemAndCategoryAndSerialAndIdNot(Long companyId, Integer system, Integer category, String serial, Long id);

    // Tìm mẫu VAT đang hoạt động mới nhất của công ty
    FormInvoiceEntity findTopByCompanyIdAndStatusAndCategoryOrderByUpdatedAtDesc(Long companyId, Integer status, Integer category);
}