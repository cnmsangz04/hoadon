package vn.hoadon.invoice.template.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceTemplateValidationRunRepository extends JpaRepository<InvoiceTemplateValidationRunEntity, Long> {
    List<InvoiceTemplateValidationRunEntity> findByDraftIdOrderByIdDesc(Long draftId);
    void deleteByDraftId(Long draftId);
}
