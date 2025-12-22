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
import java.util.*;

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
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }

                if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
                    String like = "%" + filter.getKeyword() + "%";
                    predicates.add(cb.or(
                        cb.like(root.get("domain"), like),
                        cb.like(root.get("taxcode"), like)
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
            if (company.getPrefix() == null || company.getPrefix().isEmpty()) {
                company.setPrefix(generatePrefix());
            }
            return repo.save(company);
        }

        CompanyEntity existing = repo.findById(company.getId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        if (company.getDomain() != null) existing.setDomain(company.getDomain());
        if (company.getDomainLookup() != null) existing.setDomainLookup(company.getDomainLookup());
        if (company.getTaxcode() != null) existing.setTaxcode(company.getTaxcode());
        if (company.getEmail() != null) existing.setEmail(company.getEmail());
        if (company.getHotline() != null) existing.setHotline(company.getHotline());
        if (company.getStatus() != null) existing.setStatus(company.getStatus());

        if (company.getPassword() != null && !company.getPassword().isEmpty()) {
            existing.setPassword(company.getPassword());
        }

        return repo.save(existing);
    }
    
    private String generatePrefix() {
        String prefix;
        do {
            prefix = "HD" + System.currentTimeMillis();
        } while (repo.existsByPrefix(prefix));
        return prefix;
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