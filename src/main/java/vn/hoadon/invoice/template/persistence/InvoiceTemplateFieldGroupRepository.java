package vn.hoadon.invoice.template.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceTemplateFieldGroupRepository
        extends JpaRepository<InvoiceTemplateFieldGroupEntity, Long> {
    java.util.List<InvoiceTemplateFieldGroupEntity> findAllByOrderBySortOrderAscIdAsc();

    java.util.Optional<InvoiceTemplateFieldGroupEntity> findByCode(String code);
}
