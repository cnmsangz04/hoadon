package vn.hoadon.services;

import vn.hoadon.dto.buy_invoice.BuyInvoiceFilterDTO;
import vn.hoadon.entity.BuyInvoiceEntity;
import org.springframework.data.domain.*;

import java.util.Optional;

public interface BuyInvoiceService {

    Page<BuyInvoiceEntity> list(BuyInvoiceFilterDTO filter, Pageable pageable);

    Optional<BuyInvoiceEntity> findById(Long id);

    BuyInvoiceEntity saveOrUpdate(BuyInvoiceEntity entity);

    void delete(Long id);
}
