package vn.hoadon.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.hoadon.entity.FormInvoiceEntity;

public interface FormInvoiceRepository extends JpaRepository<FormInvoiceEntity, Long>, JpaSpecificationExecutor<FormInvoiceEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FormInvoiceEntity f where f.id = :id")
    java.util.Optional<FormInvoiceEntity> lockById(@Param("id") Long id);
    @Modifying(flushAutomatically = true)
    @Query("update FormInvoiceEntity f set f.status = 0 where f.companyId = :companyId and f.system = 1 and f.category = :category and f.id <> :id and f.status = 1")
    int deactivateOthers(@Param("companyId") Long companyId, @Param("category") Integer category, @Param("id") Long id);

    FormInvoiceEntity findTopByCompanyIdAndSystemAndStatusAndCategoryOrderByUpdatedAtDesc(Long companyId, Integer system, Integer status, Integer category);
    java.util.List<FormInvoiceEntity> findByCompanyIdAndSystemAndCategoryAndStatus(Long companyId, Integer system, Integer category, Integer status);
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
