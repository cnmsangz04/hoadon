package vn.hoadon.services.impl;

import vn.hoadon.dto.buyinvoice.BuyInvoiceFilterDTO;
import vn.hoadon.dto.buyinvoice.BuyInvoiceListItemDTO;
import vn.hoadon.entity.BuyInvoiceEntity;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.repositories.BuyInvoiceRepository;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.services.BuyInvoiceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BuyInvoiceServiceImpl implements BuyInvoiceService {

    @Autowired
    private BuyInvoiceRepository repository;

    @Autowired
    private CompanyRepository companyRepository;

    @Override
    public Page<BuyInvoiceListItemDTO> list(BuyInvoiceFilterDTO filter, Pageable pageable) {
        Specification<BuyInvoiceEntity> spec = (root, query, cb) -> {
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

        Page<BuyInvoiceEntity> page = repository.findAll(spec, pageable);
        List<BuyInvoiceEntity> entities = page.getContent();

        // Collect company IDs
        Set<Long> ids = entities.stream()
                .map(BuyInvoiceEntity::getCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> nameById = new HashMap<>();
        if (!ids.isEmpty()) {
            // Bulk load companies
            List<CompanyEntity> companies = companyRepository.findAllById(ids);
            for (CompanyEntity c : companies) {
                nameById.put(c.getId(), Optional.ofNullable(c.getName()).orElse(Optional.ofNullable(c.getDomain()).orElse("#" + c.getId())));
            }
        }

        List<BuyInvoiceListItemDTO> dtos = entities.stream().map(e -> {
            String companyName = e.getCompanyId() != null ? nameById.getOrDefault(e.getCompanyId(), "") : "";
            BuyInvoiceListItemDTO dto = new BuyInvoiceListItemDTO();
            dto.setId(e.getId());
            dto.setCompanyId(e.getCompanyId());
            dto.setCompanyName(companyName);
            dto.setAmount(e.getAmount());
            dto.setAmountUsed(Optional.ofNullable(e.getAmountUsed()).orElse(0));
            dto.setStatus(e.getStatus());
            dto.setCreatedAt(e.getCreatedAt());
            dto.setUpdatedAt(e.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
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