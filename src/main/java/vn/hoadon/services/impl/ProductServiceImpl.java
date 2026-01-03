package vn.hoadon.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoadon.dto.product.ProductFilterDTO;
import vn.hoadon.entity.ProductsEntity;
import vn.hoadon.repositories.ProductRepository;
import vn.hoadon.services.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

	private final ProductRepository productRepository;

	public ProductServiceImpl(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Override
	public Page<ProductsEntity> list(ProductFilterDTO filter, Pageable pageable) {
		Specification<ProductsEntity> spec = (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (filter.getCompanyId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("companyId"), filter.getCompanyId()));
            }
			if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
				String like = "%" + filter.getKeyword().trim() + "%";
				predicates.add(criteriaBuilder.or(criteriaBuilder.like(root.get("name"), like),
						criteriaBuilder.like(root.get("code"), like)));
			}
			if (filter.getStatus() != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
			}
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
		return productRepository.findAll(spec, pageable);
	}

	@Override
	public List<ProductsEntity> list(Integer status) {
		if (status == null)
			return productRepository.findAll();
		return productRepository.findByStatus(status);
	}

	@Override
	@Transactional
	public ProductsEntity saveOrUpdate(ProductsEntity product) {
		if (product.getId() != null) {
			return productRepository.findById(product.getId()).map(t -> {
				t.setCode(product.getCode());
				t.setCompanyId(product.getCompanyId());
				t.setName(product.getName());
				t.setStatus(product.getStatus());
				t.setCompanyId(product.getCompanyId());
				t.setDescription(product.getDescription());
				t.setPrice(product.getPrice());
				t.setUnit(product.getUnit());
				t.setVatRate(product.getVatRate());
				return productRepository.save(t);
			}).orElseGet((() -> productRepository.save(product)));
		}
		return productRepository.save(product);
	}

	@Override
	public void setLock(Long id, boolean lock) {
		productRepository.findById(id).ifPresent(t -> {
			t.setStatus(lock ? 0 : 1);
			productRepository.save(t);
		});
	}

	@Override
	public Optional<ProductsEntity> finbyCode(String code) {
		if (code != null && !code.isBlank()) {
			return Optional.empty();
		}
		List<ProductsEntity> list = productRepository.findByCode(code.trim());
		return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
	}

	@Override
	public Optional<ProductsEntity> findbyName(String name) {
		if (name != null && !name.isBlank()) {
			return Optional.empty();
		}
		List<ProductsEntity> list = productRepository.findByCode(name.trim());
		return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
	}

}
