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
        validateUniqueCode(incoming);
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

    private void validateUniqueCode(CustomersEntity incoming) {
        if (incoming == null) {
            throw new IllegalArgumentException("Thiếu thông tin khách hàng");
        }
        if (incoming.getCompanyId() == null) {
            throw new IllegalArgumentException("Không xác định được công ty");
        }
        String code = incoming.getCode() != null ? incoming.getCode().trim() : "";
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mã khách hàng");
        }
        incoming.setCode(code);
        if (incoming.getId() != null) {
            CustomersEntity existing = customerRepository.findById(incoming.getId()).orElse(null);
            if (existing != null && sameCode(existing.getCode(), code)) {
                return;
            }
        }
        List<CustomersEntity> duplicates = customerRepository.findByCompanyIdAndCode(incoming.getCompanyId(), code);
        boolean hasDuplicate = duplicates.stream()
                .anyMatch(item -> item.getId() != null && !item.getId().equals(incoming.getId()));
        if (hasDuplicate) {
            throw new IllegalArgumentException("Mã khách hàng " + code + " đã tồn tại");
        }
    }

    private boolean sameCode(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

	@Override
	public List<CustomersEntity> list(Integer status) {
		if (status == null)
			return customerRepository.findAll();
		return customerRepository.findByStatus(status);
	}

    @Override
    @Transactional
    public void delete(Long id, Long companyId) {
        if (id == null) {
            throw new IllegalArgumentException("Thiếu ID khách hàng");
        }
        if (companyId == null) {
            throw new IllegalArgumentException("Không xác định được công ty");
        }
        CustomersEntity existing = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));
        if (!companyId.equals(existing.getCompanyId())) {
            throw new IllegalArgumentException("Không tìm thấy khách hàng");
        }
        customerRepository.delete(existing);
    }

}
