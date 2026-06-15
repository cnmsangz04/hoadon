package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.hoadon.entity.BuyInvoiceHistoryEntity;

public interface BuyInvoiceHistoryRepository
        extends JpaRepository<BuyInvoiceHistoryEntity, Long>,
                JpaSpecificationExecutor<BuyInvoiceHistoryEntity> {
}
