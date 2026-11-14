package vn.hoadon.invoice.template.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InvoiceTemplateDraftRepository extends JpaRepository<InvoiceTemplateDraftEntity, Long> {
    Optional<InvoiceTemplateDraftEntity> findByTemplateId(Long templateId);
}
