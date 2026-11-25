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
    java.util.List<FormInvoiceEntity> findByTemplateId(Long templateId);

    boolean existsByPhoto(String photo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FormInvoiceEntity f where f.id = :id")
    java.util.Optional<FormInvoiceEntity> lockById(@Param("id") Long id);
    @Modifying(flushAutomatically = true)
    @Query("update FormInvoiceEntity f set f.status = 0 where f.companyId = :companyId and f.system = 0 and f.category = :category and f.id <> :id and f.status = 1")
    int deactivateOthers(@Param("companyId") Long companyId, @Param("category") Integer category, @Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("update FormInvoiceEntity f set f.status = 0 where f.companyId = :companyId and f.system = 0 and f.category = :category and f.stockIssueType = :stockIssueType and f.id <> :id and f.status = 1")
    int deactivateOthersByStockIssueType(@Param("companyId") Long companyId, @Param("category") Integer category, @Param("stockIssueType") Integer stockIssueType, @Param("id") Long id);

    FormInvoiceEntity findTopByCompanyIdAndSystemAndStatusAndCategoryOrderByUpdatedAtDesc(Long companyId, Integer system, Integer status, Integer category);
    java.util.List<FormInvoiceEntity> findByCompanyIdAndSystemAndCategoryAndStatus(Long companyId, Integer system, Integer category, Integer status);
    java.util.List<FormInvoiceEntity> findByCompanyIdAndSystemAndCategoryAndStockIssueTypeAndStatus(Long companyId, Integer system, Integer category, Integer stockIssueType, Integer status);
    Page<FormInvoiceEntity> findByCompanyId(Long companyId, Pageable pageable);
    Page<FormInvoiceEntity> findByCompanyIdAndSystem(Long companyId, Integer system, Pageable pageable);
    
    Page<FormInvoiceEntity> findBySystem(Integer system, Pageable pageable);

    Page<FormInvoiceEntity> findByUserId(Long userId, Pageable pageable);

    boolean existsByCompanyIdAndSystemAndCategoryAndSerial(Long companyId, Integer system, Integer category, String serial);
    boolean existsByCompanyIdAndSystemAndCategoryAndSerialAndIdNot(Long companyId, Integer system, Integer category, String serial, Long id);

    FormInvoiceEntity findTopByCompanyIdAndStatusAndCategoryOrderByUpdatedAtDesc(Long companyId, Integer status, Integer category);
}
