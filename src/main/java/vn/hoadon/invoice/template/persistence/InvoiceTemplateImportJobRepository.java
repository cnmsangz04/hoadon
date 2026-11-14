package vn.hoadon.invoice.template.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface InvoiceTemplateImportJobRepository extends JpaRepository<InvoiceTemplateImportJobEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from InvoiceTemplateImportJobEntity j where j.id = :id")
    Optional<InvoiceTemplateImportJobEntity> lockById(@Param("id") Long id);

}
