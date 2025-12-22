package vn.hoadon.repositories;

import vn.hoadon.entity.BuyInvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BuyInvoiceRepository
        extends JpaRepository<BuyInvoiceEntity, Long>,
                JpaSpecificationExecutor<BuyInvoiceEntity> {
}
