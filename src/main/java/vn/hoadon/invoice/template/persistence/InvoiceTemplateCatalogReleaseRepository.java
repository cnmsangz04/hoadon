package vn.hoadon.invoice.template.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceTemplateCatalogReleaseRepository
        extends JpaRepository<InvoiceTemplateCatalogReleaseEntity, Long> {
    java.util.Optional<InvoiceTemplateCatalogReleaseEntity> findByCode(String code);

    java.util.Optional<InvoiceTemplateCatalogReleaseEntity> findTopByOrderByIdDesc();
}
