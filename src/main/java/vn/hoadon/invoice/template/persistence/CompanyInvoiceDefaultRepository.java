package vn.hoadon.invoice.template.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CompanyInvoiceDefaultRepository extends JpaRepository<CompanyInvoiceDefaultEntity, Long> {
    Optional<CompanyInvoiceDefaultEntity> findByCompanyIdAndCategory(Long companyId, Integer category);
}
