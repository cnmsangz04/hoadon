package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hoadon.entity.InvoiceEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

    @Query("SELECT i FROM InvoiceEntity i WHERE (:companyId IS NULL OR i.companyId = :companyId) AND (:q IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(i.code) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(i.lookupCode) LIKE LOWER(CONCAT('%', :q, '%'))) AND (:status IS NULL OR i.status = :status)")
    Page<InvoiceEntity> search(@Param("companyId") Long companyId, @Param("q") String q, @Param("status") Short status, Pageable pageable);

    @Query("SELECT i FROM InvoiceEntity i WHERE (:companyId IS NULL OR i.companyId = :companyId) AND (:q IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(i.code) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(i.lookupCode) LIKE LOWER(CONCAT('%', :q, '%'))) AND (:status IS NULL OR i.status = :status) AND (:date IS NULL OR i.dateExport = :date)")
    Page<InvoiceEntity> search(@Param("companyId") Long companyId, @Param("q") String q, @Param("status") Short status, @Param("date") java.time.LocalDate date, Pageable pageable);

    boolean existsByIdAttr(String idAttr);
    boolean existsByLookupCode(String lookupCode);
    boolean existsByFormId(Integer formId);

    java.util.Optional<InvoiceEntity> findByLookupCode(String lookupCode);

    java.util.Optional<InvoiceEntity> findFirstByCompanyIdAndNoOrderByIdDesc(Integer companyId, Integer no);

    @Query("SELECT i FROM InvoiceEntity i JOIN FormInvoiceEntity f ON i.formId = f.id " +
            "WHERE (:companyId IS NULL OR i.companyId = :companyId) " +
            "AND (:category IS NULL OR f.category = :category) " +
            "AND (:status IS NULL OR i.status = :status) " +
            "AND (:fromDate IS NULL OR i.dateExport >= :fromDate) " +
            "AND (:toDate IS NULL OR i.dateExport <= :toDate)")
    Page<InvoiceEntity> searchReportInvoices(
            @Param("companyId") Long companyId,
            @Param("category") Integer category,
            @Param("status") Short status,
            @Param("fromDate") java.time.LocalDate fromDate,
            @Param("toDate") java.time.LocalDate toDate,
            Pageable pageable
    );

    // Dashboard statistics queries
    @Query("SELECT COUNT(i) FROM InvoiceEntity i WHERE i.companyId = :companyId " +
            "AND i.status IN :statuses " +
            "AND i.dateExport BETWEEN :startDate AND :endDate")
    Long countByCompanyIdAndStatusInAndDateExportBetween(
            @Param("companyId") Long companyId,
            @Param("statuses") java.util.List<Short> statuses,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(i.amount), 0.0) FROM InvoiceEntity i WHERE i.companyId = :companyId " +
            "AND i.status IN :statuses " +
            "AND i.dateExport BETWEEN :startDate AND :endDate")
    Double sumAmountByCompanyIdAndStatusInAndDateExportBetween(
            @Param("companyId") Long companyId,
            @Param("statuses") java.util.List<Short> statuses,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    interface InvoiceStatusCount {
        Integer getCompanyId();
        Short getStatus();
        Long getTotal();
    }

    @Query("SELECT i.companyId AS companyId, i.status AS status, COUNT(i) AS total " +
            "FROM InvoiceEntity i " +
            "WHERE i.status IN :statuses " +
            "AND i.updatedAt >= :fromTime " +
            "AND i.updatedAt < :toTime " +
            "GROUP BY i.companyId, i.status")
    List<InvoiceStatusCount> countStatusByUpdatedAtBetween(
            @Param("statuses") List<Short> statuses,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );
}
