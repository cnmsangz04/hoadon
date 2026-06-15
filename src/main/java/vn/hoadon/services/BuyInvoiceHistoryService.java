package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.hoadon.dto.buyinvoice.BuyInvoiceHistoryDTO;
import vn.hoadon.dto.buyinvoice.BuyInvoiceHistoryFilterDTO;
import vn.hoadon.entity.BuyInvoiceEntity;
import vn.hoadon.entity.UserEntity;

public interface BuyInvoiceHistoryService {
    void record(BuyInvoiceEntity before,
                BuyInvoiceEntity after,
                String changeType,
                String source,
                UserEntity actor,
                Long packagePurchaseId,
                String packageName,
                String paymentCode,
                String note);

    Page<BuyInvoiceHistoryDTO> list(BuyInvoiceHistoryFilterDTO filter, Pageable pageable);
}
