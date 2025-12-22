package vn.hoadon.services.impl;

import vn.hoadon.dto.buyinvoice.BuyInvoiceFilterDTO;
import vn.hoadon.entity.BuyInvoiceEntity;
import vn.hoadon.repositories.BuyInvoiceRepository;
import vn.hoadon.services.BuyInvoiceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BuyInvoiceServiceImpl implements BuyInvoiceService {

    @Autowired
    private BuyInvoiceRepository repository;

    @Override
    public Page<BuyInvoiceEntity> list(BuyInvoiceFilterDTO filter, Pageable pageable) {
        Specification<BuyInvoiceEntity> spec = (root, query, cb) -> {
            // filter theo companyId
            var predicates = cb.conjunction();
            if (filter.getCompanyId() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("companyId"), filter.getCompanyId()));
            }
            if (filter.getStatus() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                predicates = cb.and(predicates, cb.like(root.get("id").as(String.class), "%" + filter.getKeyword() + "%"));
            }
            return predicates;
        };

        return repository.findAll(spec, pageable);
    }

    @Override
    public Optional<BuyInvoiceEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public BuyInvoiceEntity saveOrUpdate(BuyInvoiceEntity entity) {
        if (entity.getId() == null) {
            entity.setCreatedAt(java.time.LocalDateTime.now());
        }
        entity.setUpdatedAt(java.time.LocalDateTime.now());
        return repository.save(entity);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
