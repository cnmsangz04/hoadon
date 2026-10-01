package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hoadon.entity.ErrorMessageEntity;

import java.time.LocalDate;
import java.util.Optional;

public interface ErrorMessageRepository extends JpaRepository<ErrorMessageEntity, Long> {
    Optional<ErrorMessageEntity> findByIdAndCompanyId(Long id, Long companyId);

    @Query("""
            SELECT e
            FROM ErrorMessageEntity e
            WHERE e.companyId = :companyId
              AND (:status IS NULL OR e.status = :status)
              AND (:notificationType IS NULL OR e.notificationType = :notificationType)
              AND (:dateFrom IS NULL OR e.noticeDate >= :dateFrom)
              AND (:dateTo IS NULL OR e.noticeDate <= :dateTo)
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(e.formPattern) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(e.messageCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(e.taxResponseNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(e.taxpayerName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<ErrorMessageEntity> search(@Param("companyId") Long companyId,
                                    @Param("keyword") String keyword,
                                    @Param("status") Integer status,
                                    @Param("notificationType") Integer notificationType,
                                    @Param("dateFrom") LocalDate dateFrom,
                                    @Param("dateTo") LocalDate dateTo,
                                    Pageable pageable);
}
