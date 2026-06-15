package vn.hoadon.repositories;

import vn.hoadon.entity.BuyInvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface BuyInvoiceRepository
        extends JpaRepository<BuyInvoiceEntity, Long>,
                JpaSpecificationExecutor<BuyInvoiceEntity> {
    
    Optional<BuyInvoiceEntity> findFirstByCompanyIdAndStatusOrderByIdDesc(Long companyId, Integer status);

    Optional<BuyInvoiceEntity> findFirstByCompanyIdOrderByIdDesc(Long companyId);
}
