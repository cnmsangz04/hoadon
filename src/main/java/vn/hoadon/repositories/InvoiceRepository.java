package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hoadon.entity.InvoiceEntity;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

    @Query("SELECT i FROM InvoiceEntity i WHERE (:q IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(i.code) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(i.lookupCode) LIKE LOWER(CONCAT('%', :q, '%'))) AND (:status IS NULL OR i.status = :status)")
    Page<InvoiceEntity> search(@Param("q") String q, @Param("status") Short status, Pageable pageable);

    @Query("SELECT i FROM InvoiceEntity i WHERE (:q IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(i.code) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(i.lookupCode) LIKE LOWER(CONCAT('%', :q, '%'))) AND (:status IS NULL OR i.status = :status) AND (:date IS NULL OR i.dateExport = :date)")
    Page<InvoiceEntity> search(@Param("q") String q, @Param("status") Short status, @Param("date") java.time.LocalDate date, Pageable pageable);

    boolean existsByIdAttr(String idAttr);
    boolean existsByLookupCode(String lookupCode);
    boolean existsByFormId(Integer formId);

    java.util.Optional<InvoiceEntity> findByLookupCode(String lookupCode);

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
}