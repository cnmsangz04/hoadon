package vn.hoadon.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hoadon.entity.MailJobEntity;

import java.util.List;

public interface MailJobRepository extends JpaRepository<MailJobEntity, Long> {

    /**
     * Lấy các job còn pending: chưa thất bại, chưa được reserved, đến giờ xử lý.
     * Sắp xếp theo createdAt ASC để xử lý theo thứ tự vào trước ra trước.
     */
    @Query("SELECT j FROM MailJobEntity j " +
           "WHERE j.failed = false " +
           "  AND j.reservedAt IS NULL " +
           "  AND j.sentAt IS NULL " +
           "  AND (j.status IS NULL OR j.status IN ('queued', 'retry')) " +
           "  AND j.availableAt <= :now " +
           "ORDER BY j.createdAt ASC")
    List<MailJobEntity> findAvailableJobs(@Param("now") long now, Pageable pageable);

    List<MailJobEntity> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    @Query("SELECT j FROM MailJobEntity j " +
           "WHERE (:companyId IS NULL OR j.companyId = :companyId) " +
           "  AND j.showHistory = true " +
           "  AND (:status IS NULL OR j.status = :status) " +
           "  AND (:q IS NULL OR LOWER(COALESCE(j.toEmail, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "       OR LOWER(COALESCE(j.toName, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "       OR LOWER(COALESCE(j.subject, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "       OR LOWER(COALESCE(j.templateKey, '')) LIKE LOWER(CONCAT('%', :q, '%'))) ")
    Page<MailJobEntity> searchHistory(@Param("companyId") Long companyId,
                                      @Param("status") String status,
                                      @Param("q") String q,
                                      Pageable pageable);

    @Query("SELECT j FROM MailJobEntity j " +
           "WHERE (:status IS NULL OR j.status = :status) " +
           "  AND (:q IS NULL OR LOWER(COALESCE(j.toEmail, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "       OR LOWER(COALESCE(j.toName, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "       OR LOWER(COALESCE(j.subject, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "       OR LOWER(COALESCE(j.templateKey, '')) LIKE LOWER(CONCAT('%', :q, '%'))) ")
    Page<MailJobEntity> searchAllHistory(@Param("status") String status,
                                         @Param("q") String q,
                                         Pageable pageable);
}
