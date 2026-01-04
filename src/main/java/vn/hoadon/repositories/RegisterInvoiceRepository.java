package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.hoadon.entity.RegisterInvoiceEntity;

import java.util.List;

@Repository
public interface RegisterInvoiceRepository extends JpaRepository<RegisterInvoiceEntity, Long> {
    List<RegisterInvoiceEntity> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<RegisterInvoiceEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Pagination variants
    Page<RegisterInvoiceEntity> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);
    Page<RegisterInvoiceEntity> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, Integer status, Pageable pageable);

    Page<RegisterInvoiceEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<RegisterInvoiceEntity> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Integer status, Pageable pageable);
}