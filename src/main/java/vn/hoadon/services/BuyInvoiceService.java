package vn.hoadon.services;

import vn.hoadon.dto.buyinvoice.BuyInvoiceFilterDTO;
import vn.hoadon.dto.buyinvoice.BuyInvoiceListItemDTO;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.List;

public interface BuyInvoiceService {

    Page<BuyInvoiceListItemDTO> list(BuyInvoiceFilterDTO filter, Pageable pageable);

    Optional<vn.hoadon.entity.BuyInvoiceEntity> findById(Long id);

    vn.hoadon.entity.BuyInvoiceEntity saveOrUpdate(vn.hoadon.entity.BuyInvoiceEntity entity);

    void delete(Long id);
    
    List<vn.hoadon.entity.BuyInvoiceEntity> findAll(Specification<vn.hoadon.entity.BuyInvoiceEntity> spec);
}