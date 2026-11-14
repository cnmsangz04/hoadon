package vn.hoadon.invoice.template.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceTemplateDatasetRepository
        extends JpaRepository<InvoiceTemplateDatasetEntity, Long> {
    java.util.Optional<InvoiceTemplateDatasetEntity> findByCode(String code);
}
