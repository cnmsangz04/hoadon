package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.InvoiceImportEntity;

import java.util.Optional;

public interface InvoiceImportRepository extends JpaRepository<InvoiceImportEntity, Long> {
    Page<InvoiceImportEntity> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);
    Optional<InvoiceImportEntity> findByIdAndCompanyId(Long id, Long companyId);
}
