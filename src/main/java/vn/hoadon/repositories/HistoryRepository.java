package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hoadon.entity.HistoryEntity;

public interface HistoryRepository extends JpaRepository<HistoryEntity, Long> {
    Page<HistoryEntity> findByCompanyIdAndShowNotifyAndStatusOrderByCreatedAtDescIdDesc(
            Long companyId,
            Integer showNotify,
            Integer status,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(h)
            FROM HistoryEntity h
            WHERE h.companyId = :companyId
              AND h.showNotify = :showNotify
              AND h.status = :status
              AND NOT EXISTS (
                  SELECT r.id
                  FROM NotificationReadEntity r
                  WHERE r.userId = :userId
                    AND r.historyId = h.id
              )
            """)
    long countUnreadNotifications(@Param("companyId") Long companyId,
                                  @Param("userId") Long userId,
                                  @Param("showNotify") Integer showNotify,
                                  @Param("status") Integer status);
}
