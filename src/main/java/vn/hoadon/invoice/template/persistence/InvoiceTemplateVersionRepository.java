package vn.hoadon.invoice.template.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InvoiceTemplateVersionRepository extends JpaRepository<InvoiceTemplateVersionEntity, Long> {
    List<InvoiceTemplateVersionEntity> findByTemplateIdOrderByVersionNoDesc(Long templateId);
    Optional<InvoiceTemplateVersionEntity> findTopByTemplateIdOrderByVersionNoDesc(Long templateId);
}
