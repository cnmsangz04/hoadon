package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.HashInvoiceEntity;

import java.util.Optional;

public interface HashInvoiceRepository extends JpaRepository<HashInvoiceEntity, Long> {
    Optional<HashInvoiceEntity> findByHash(String hash);
}
