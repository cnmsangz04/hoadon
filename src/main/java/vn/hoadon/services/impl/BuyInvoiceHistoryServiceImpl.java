package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.dto.buyinvoice.BuyInvoiceHistoryDTO;
import vn.hoadon.dto.buyinvoice.BuyInvoiceHistoryFilterDTO;
import vn.hoadon.entity.BuyInvoiceEntity;
import vn.hoadon.entity.BuyInvoiceHistoryEntity;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.BuyInvoiceHistoryRepository;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.services.BuyInvoiceHistoryService;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class BuyInvoiceHistoryServiceImpl implements BuyInvoiceHistoryService {

    @Autowired
    private BuyInvoiceHistoryRepository repository;

    @Autowired
    private CompanyRepository companyRepository;

    @Override
    @Transactional
    public void record(BuyInvoiceEntity before,
                       BuyInvoiceEntity after,
                       String changeType,
                       String source,
                       UserEntity actor,
                       Long packagePurchaseId,
                       String packageName,
                       String paymentCode,
                       String note) {
        Long companyId = after != null ? after.getCompanyId() : before != null ? before.getCompanyId() : null;
        Long buyInvoiceId = after != null ? after.getId() : before != null ? before.getId() : null;

        BuyInvoiceHistoryEntity entity = new BuyInvoiceHistoryEntity();
        entity.setBuyInvoiceId(buyInvoiceId);
        entity.setCompanyId(companyId);
        resolveCompany(companyId).ifPresent(company -> {
            entity.setCompanyName(company.getName());
            entity.setCompanyTaxcode(company.getTaxcode());
        });
        entity.setChangeType(changeType);
        entity.setSource(source);
        entity.setPreviousAmount(amount(before));
        entity.setNewAmount(amount(after));
        entity.setAmountDelta(amount(after) - amount(before));
        entity.setPreviousAmountUsed(amountUsed(before));
        entity.setNewAmountUsed(amountUsed(after));
        entity.setPreviousStatus(status(before));
        entity.setNewStatus(status(after));
        entity.setPackagePurchaseId(packagePurchaseId);
        entity.setPackageName(packageName);
        entity.setPaymentCode(paymentCode);
        entity.setActorUserId(actor != null ? actor.getId() : null);
        entity.setActorName(resolveActorName(actor));
        entity.setActorUsername(actor != null ? actor.getUsername() : null);
        entity.setNote(note);
        entity.setCreatedAt(LocalDateTime.now());
        repository.save(entity);
    }

    @Override
    public Page<BuyInvoiceHistoryDTO> list(BuyInvoiceHistoryFilterDTO filter, Pageable pageable) {
        return repository.findAll(spec(filter), pageable).map(this::toDto);
    }

    private Specification<BuyInvoiceHistoryEntity> spec(BuyInvoiceHistoryFilterDTO filter) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (filter != null && filter.getCompanyId() != null) {
                predicates.add(cb.equal(root.get("companyId"), filter.getCompanyId()));
            }
            if (filter != null && filter.getSource() != null && !filter.getSource().isBlank()) {
                predicates.add(cb.equal(root.get("source"), filter.getSource()));
            }
            if (filter != null && filter.getChangeType() != null && !filter.getChangeType().isBlank()) {
                predicates.add(cb.equal(root.get("changeType"), filter.getChangeType()));
            }
            if (filter != null && filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate().atStartOfDay()));
            }
            if (filter != null && filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getToDate().atTime(LocalTime.MAX)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private BuyInvoiceHistoryDTO toDto(BuyInvoiceHistoryEntity entity) {
        BuyInvoiceHistoryDTO dto = new BuyInvoiceHistoryDTO();
        dto.setId(entity.getId());
        dto.setBuyInvoiceId(entity.getBuyInvoiceId());
        dto.setCompanyId(entity.getCompanyId());
        dto.setCompanyName(entity.getCompanyName());
        dto.setCompanyTaxcode(entity.getCompanyTaxcode());
        dto.setChangeType(entity.getChangeType());
        dto.setSource(entity.getSource());
        dto.setPreviousAmount(entity.getPreviousAmount());
        dto.setNewAmount(entity.getNewAmount());
        dto.setAmountDelta(entity.getAmountDelta());
        dto.setPreviousAmountUsed(entity.getPreviousAmountUsed());
        dto.setNewAmountUsed(entity.getNewAmountUsed());
        dto.setPreviousStatus(entity.getPreviousStatus());
        dto.setNewStatus(entity.getNewStatus());
        dto.setPackagePurchaseId(entity.getPackagePurchaseId());
        dto.setPackageName(entity.getPackageName());
        dto.setPaymentCode(entity.getPaymentCode());
        dto.setActorUserId(entity.getActorUserId());
        dto.setActorName(entity.getActorName());
        dto.setActorUsername(entity.getActorUsername());
        dto.setNote(entity.getNote());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private Optional<CompanyEntity> resolveCompany(Long companyId) {
        return companyId != null ? companyRepository.findById(companyId) : Optional.empty();
    }

    private String resolveActorName(UserEntity actor) {
        if (actor == null) return null;
        if (actor.getName() != null && !actor.getName().isBlank()) return actor.getName();
        return actor.getUsername();
    }

    private Integer amount(BuyInvoiceEntity entity) {
        return entity != null && entity.getAmount() != null ? entity.getAmount() : 0;
    }

    private Integer amountUsed(BuyInvoiceEntity entity) {
        return entity != null && entity.getAmountUsed() != null ? entity.getAmountUsed() : 0;
    }

    private Integer status(BuyInvoiceEntity entity) {
        return entity != null ? entity.getStatus() : null;
    }
}
