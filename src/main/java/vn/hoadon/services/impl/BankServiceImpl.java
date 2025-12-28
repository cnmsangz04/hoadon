package vn.hoadon.services.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoadon.dto.bank.BankFilterDTO;
import vn.hoadon.entity.BankEntity;
import vn.hoadon.repositories.BankRepository;
import vn.hoadon.services.BankService;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BankServiceImpl implements BankService {

    private final BankRepository bankRepository;

    public BankServiceImpl(BankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    @Override
    public List<BankEntity> list(Integer status) {
        if (status == null) return bankRepository.findAll();
        return bankRepository.findByStatus(status);
    }

    @Override
    public Optional<BankEntity> findByAbbreviation(String abbreviation) {
        if (abbreviation == null || abbreviation.isBlank()) return Optional.empty();
        List<BankEntity> list = bankRepository.findByAbbreviation(abbreviation);
        if (list == null || list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(0));
    }

    @Override
    public Optional<BankEntity> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        List<BankEntity> list = bankRepository.findByName(name);
        if (list == null || list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(0));
    }
    
    @Override
    public Page<BankEntity> list(BankFilterDTO filter, Pageable pageable) {
        Specification<BankEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String like = "%" + filter.getKeyword().trim() + "%";
                predicates.add(
                    cb.or(
                        cb.like(root.get("name"), like),
                        cb.like(root.get("abbreviation"), like)
                    )
                );
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return bankRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public BankEntity saveOrUpdate(BankEntity incoming) {
        if (incoming.getId() != null) {
            return bankRepository.findById(incoming.getId()).map(existing -> {
                existing.setName(incoming.getName());
                existing.setAbbreviation(incoming.getAbbreviation());
                existing.setStatus(incoming.getStatus());
                return bankRepository.save(existing);
            }).orElseGet(() -> bankRepository.save(incoming));
        }
        return bankRepository.save(incoming);
    }


    @Override
    @Transactional
    public void setLock(Long id, boolean lock) {
        bankRepository.findById(id).ifPresent(bank -> {
            bank.setStatus(lock ? 0 : 1);
            bankRepository.save(bank);
        });
    }
}