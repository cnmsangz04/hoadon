package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hoadon.entity.InvoiceEntity;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

    @Query("SELECT i FROM InvoiceEntity i WHERE (:q IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(i.code) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(i.lookupCode) LIKE LOWER(CONCAT('%', :q, '%'))) AND (:status IS NULL OR i.status = :status)")
    Page<InvoiceEntity> search(@Param("q") String q, @Param("status") Short status, Pageable pageable);
}
