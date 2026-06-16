package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.NotificationReadEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationReadRepository extends JpaRepository<NotificationReadEntity, Long> {
    List<NotificationReadEntity> findByUserIdAndHistoryIdIn(Long userId, Collection<Long> historyIds);
    Optional<NotificationReadEntity> findByUserIdAndHistoryId(Long userId, Long historyId);
}
