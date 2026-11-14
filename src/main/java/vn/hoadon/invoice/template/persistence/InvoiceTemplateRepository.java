package vn.hoadon.invoice.template.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface InvoiceTemplateRepository extends JpaRepository<InvoiceTemplateEntity, Long>, JpaSpecificationExecutor<InvoiceTemplateEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from InvoiceTemplateEntity t where t.id = :id")
    Optional<InvoiceTemplateEntity> lockById(@Param("id") Long id);
}
