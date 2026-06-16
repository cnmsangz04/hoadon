package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.LoginSessionEntity;

import java.util.List;
import java.util.Optional;

public interface LoginSessionRepository extends JpaRepository<LoginSessionEntity, Long> {
    Optional<LoginSessionEntity> findBySessionId(String sessionId);
    List<LoginSessionEntity> findByUserIdOrderByLastSeenAtDesc(Long userId);
    List<LoginSessionEntity> findByUserIdAndLoginTypeOrderByLastSeenAtDesc(Long userId, String loginType);
}
