package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.hoadon.entity.InvoicePackagePurchaseEntity;

public interface InvoicePackagePurchaseRepository extends JpaRepository<InvoicePackagePurchaseEntity, Long>,
        JpaSpecificationExecutor<InvoicePackagePurchaseEntity> {

    Page<InvoicePackagePurchaseEntity> findByCompanyId(Long companyId, Pageable pageable);
}
