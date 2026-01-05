package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.hoadon.entity.RegisterInvoiceEntity;

import java.util.List;

@Repository
public interface RegisterInvoiceRepository extends JpaRepository<RegisterInvoiceEntity, Long> {
    List<RegisterInvoiceEntity> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<RegisterInvoiceEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Pagination variants
    Page<RegisterInvoiceEntity> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);
    Page<RegisterInvoiceEntity> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, Integer status, Pageable pageable);

    Page<RegisterInvoiceEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<RegisterInvoiceEntity> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Integer status, Pageable pageable);

    // New: latest accepted by effectiveDate desc
    Page<RegisterInvoiceEntity> findByCompanyIdAndStatusOrderByEffectiveDateDesc(Long companyId, Integer status, Pageable pageable);
    RegisterInvoiceEntity findTopByCompanyIdAndStatusOrderByEffectiveDateDesc(Long companyId, Integer status);

    // Deterministic latest accepted: order by effectiveDate DESC, createdAt DESC, use Pageable to limit
    @Query("SELECT r FROM RegisterInvoiceEntity r WHERE r.companyId = :companyId AND r.status = 6 ORDER BY CASE WHEN r.effectiveDate IS NULL THEN 1 ELSE 0 END, r.effectiveDate DESC, r.createdAt DESC")
    List<RegisterInvoiceEntity> findLatestAccepted(@Param("companyId") Long companyId, Pageable pageable);
}