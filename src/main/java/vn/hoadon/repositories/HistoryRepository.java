package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.HistoryEntity;

public interface HistoryRepository extends JpaRepository<HistoryEntity, Long> {
}
