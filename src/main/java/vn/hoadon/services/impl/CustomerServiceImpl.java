package vn.hoadon.services.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import vn.hoadon.dto.customer.CustomerFilterDTO;
import vn.hoadon.entity.CustomersEntity;
import vn.hoadon.repositories.CustomerRepository;
import vn.hoadon.services.CustomerService;

@Service
public class CustomerServiceImpl implements CustomerService{
	
	@Autowired
    private CustomerRepository customerRepository;
	
	public CustomerServiceImpl(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	@Override
	public Page<CustomersEntity> list(CustomerFilterDTO filter, Pageable pageable) {
		Specification<CustomersEntity> spec = (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (filter.getCompanyId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("companyId"), filter.getCompanyId()));
            }
			if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
				String like = "%" + filter.getKeyword().trim() + "%";
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(root.get("code"), like)));
			}
			if (filter.getStatus() != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
			}
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
		return customerRepository.findAll(spec, pageable);
	}

	@Override
    @Transactional
    public CustomersEntity saveOrUpdate(CustomersEntity incoming) {
        if (incoming.getId() != null) {
            return customerRepository.findById(incoming.getId()).map(existing -> {
                existing.setCode(incoming.getCode());
                existing.setTaxCode(incoming.getTaxCode());
                existing.setCompanyName(incoming.getCompanyName());
                existing.setBuyerName(incoming.getBuyerName());
                existing.setAddress(incoming.getAddress());
                existing.setPhone(incoming.getPhone());
                existing.setEmail(incoming.getEmail());
                existing.setFax(incoming.getFax());
                existing.setBankName(incoming.getBankName());
                existing.setBankAccountNumber(incoming.getBankAccountNumber());
                existing.setDescription(incoming.getDescription());
                existing.setStatus(incoming.getStatus());
                return customerRepository.save(existing);
            }).orElseGet(() -> customerRepository.save(incoming));
        }
        return customerRepository.save(incoming);
    }

	@Override
	public List<CustomersEntity> list(Integer status) {
		if (status == null)
			return customerRepository.findAll();
		return customerRepository.findByStatus(status);
	}

}
