package vn.hoadon.services.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.services.CompanyService;
import vn.hoadon.dto.company.CompanyFilterDTO;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository repo;

    public CompanyServiceImpl(CompanyRepository repo) {
        this.repo = repo;
    }

    @Override
    public Page<CompanyEntity> list(CompanyFilterDTO filter, Pageable pageable) {
        Specification<CompanyEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter != null) {
                if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }

                if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
                    predicates.add(cb.or(
                        cb.like(root.get("domain"), "%" + filter.getKeyword() + "%"),
                        cb.like(root.get("taxcode"), "%" + filter.getKeyword() + "%")
                    ));
                }

                if (filter.getCompanyId() != null) {
                    predicates.add(cb.equal(root.get("id"), filter.getCompanyId()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repo.findAll(spec, pageable);
    }

    @Override
    public CompanyEntity saveOrUpdate(CompanyEntity company) {
        if (company.getId() == null) {
            // Create new
            company.setCreatedAt(LocalDateTime.now());
            company.setUpdatedAt(LocalDateTime.now());
            return repo.save(company);
        } else {
            // Update existing: merge fields
            Optional<CompanyEntity> opt = repo.findById(company.getId());
            CompanyEntity existing = opt.orElseGet(CompanyEntity::new);

            // preserve createdAt if exists
            if (existing.getCreatedAt() != null) {
                company.setCreatedAt(existing.getCreatedAt());
            } else {
                company.setCreatedAt(company.getCreatedAt());
            }

            // Merge field-by-field only when provided (non-null)
            if (company.getDomain() != null) existing.setDomain(company.getDomain());
            if (company.getDomainLookup() != null) existing.setDomainLookup(company.getDomainLookup());
            if (company.getPrefix() != null) existing.setPrefix(company.getPrefix());
            if (company.getTaxcode() != null) existing.setTaxcode(company.getTaxcode());
            if (company.getEmail() != null) existing.setEmail(company.getEmail());
            if (company.getHotline() != null) existing.setHotline(company.getHotline());
            if (company.getStatus() != null) existing.setStatus(company.getStatus());
            // Password: only update if provided (non-null and not empty)
            if (company.getPassword() != null && !company.getPassword().isEmpty()) {
                existing.setPassword(company.getPassword());
            }

            existing.setUpdatedAt(LocalDateTime.now());

            // ensure id stays
            existing.setId(company.getId());

            return repo.save(existing);
        }
    }

    @Override
    public Optional<CompanyEntity> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }
}