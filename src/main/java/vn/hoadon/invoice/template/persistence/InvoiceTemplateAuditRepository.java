package vn.hoadon.invoice.template.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InvoiceTemplateAuditRepository extends JpaRepository<InvoiceTemplateAuditEntity, Long> {
    Optional<InvoiceTemplateAuditEntity> findByRequestKey(String requestKey);
}
