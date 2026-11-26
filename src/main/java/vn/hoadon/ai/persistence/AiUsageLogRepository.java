package vn.hoadon.ai.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLogEntity, Long> {
    @Query("""
            SELECT l
            FROM AiUsageLogEntity l
            WHERE (:companyId IS NULL OR l.companyId = :companyId)
              AND l.createdAt >= :from
              AND l.createdAt < :to
            ORDER BY l.createdAt DESC, l.id DESC
            """)
    Page<AiUsageLogEntity> findUsage(@Param("companyId") Long companyId,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to,
                                     Pageable pageable);

    @Query("""
            SELECT COUNT(l) AS requestCount,
                   COALESCE(SUM(CASE WHEN l.status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS successCount,
                   COALESCE(SUM(CASE WHEN l.status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failureCount,
                   COALESCE(SUM(l.inputTokens), 0) AS inputTokens,
                   COALESCE(SUM(l.outputTokens), 0) AS outputTokens,
                   COALESCE(SUM(l.totalTokens), 0) AS totalTokens
            FROM AiUsageLogEntity l
            WHERE (:companyId IS NULL OR l.companyId = :companyId)
              AND l.createdAt >= :from
              AND l.createdAt < :to
            """)
    AiUsageAggregate aggregateUsage(@Param("companyId") Long companyId,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);
}
