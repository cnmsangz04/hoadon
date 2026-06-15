package vn.hoadon.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import vn.hoadon.entity.InvoicePackagePurchaseEntity;

import java.util.Optional;

public interface InvoicePackagePurchaseRepository extends JpaRepository<InvoicePackagePurchaseEntity, Long>,
        JpaSpecificationExecutor<InvoicePackagePurchaseEntity> {

    Page<InvoicePackagePurchaseEntity> findByCompanyId(Long companyId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InvoicePackagePurchaseEntity> findByPaymentCode(String paymentCode);
}
