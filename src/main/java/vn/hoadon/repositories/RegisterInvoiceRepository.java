package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.hoadon.entity.RegisterInvoiceEntity;

import java.util.List;

@Repository
public interface RegisterInvoiceRepository extends JpaRepository<RegisterInvoiceEntity, Long> {
    List<RegisterInvoiceEntity> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<RegisterInvoiceEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}