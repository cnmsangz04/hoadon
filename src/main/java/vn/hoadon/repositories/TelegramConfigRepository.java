package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.TelegramConfigEntity;

import java.util.Optional;

public interface TelegramConfigRepository extends JpaRepository<TelegramConfigEntity, Integer> {
    Optional<TelegramConfigEntity> findTopByOrderByIdDesc();
}
