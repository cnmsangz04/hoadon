package vn.hoadon.services.impl;

import vn.hoadon.dto.buyinvoice.BuyInvoiceFilterDTO;
import vn.hoadon.entity.BuyInvoiceEntity;
import vn.hoadon.repositories.BuyInvoiceRepository;
import vn.hoadon.services.BuyInvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.*;

@Service
public class BuyInvoiceServiceImpl implements BuyInvoiceService {

    @Autowired
    private BuyInvoiceRepository repo;

    @Override
    public Page<BuyInvoiceEntity> list(BuyInvoiceFilterDTO filter, Pageable pageable) {

        Specification<BuyInvoiceEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getCompanyId() != null && filter.getCompanyId() > 0) {
                predicates.add(cb.equal(root.get("companyId"), filter.getCompanyId()));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repo.findAll(spec, pageable);
    }

    @Override
    public Optional<BuyInvoiceEntity> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public BuyInvoiceEntity saveOrUpdate(BuyInvoiceEntity entity) {
        return repo.save(entity);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }
}
