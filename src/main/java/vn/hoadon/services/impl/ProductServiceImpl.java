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
		validateUniqueCode(product);
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

	private void validateUniqueCode(ProductsEntity product) {
		if (product == null) {
			throw new IllegalArgumentException("Thiếu thông tin sản phẩm");
		}
		if (product.getCompanyId() == null) {
			throw new IllegalArgumentException("Không xác định được công ty");
		}
		String code = product.getCode() != null ? product.getCode().trim() : "";
		if (code.isEmpty()) {
			throw new IllegalArgumentException("Vui lòng nhập mã sản phẩm");
		}
		product.setCode(code);
		if (product.getId() != null) {
			ProductsEntity existing = productRepository.findById(product.getId()).orElse(null);
			if (existing != null && sameCode(existing.getCode(), code)) {
				return;
			}
		}
		List<ProductsEntity> duplicates = productRepository.findByCompanyIdAndCode(product.getCompanyId(), code);
		boolean hasDuplicate = duplicates.stream()
				.anyMatch(item -> item.getId() != null && !item.getId().equals(product.getId()));
		if (hasDuplicate) {
			throw new IllegalArgumentException("Mã sản phẩm " + code + " đã tồn tại");
		}
	}

	private boolean sameCode(String left, String right) {
		return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
	}

	@Override
	@Transactional
	public void delete(Long id, Long companyId) {
		if (id == null) {
			throw new IllegalArgumentException("Thiếu ID sản phẩm");
		}
		if (companyId == null) {
			throw new IllegalArgumentException("Không xác định được công ty");
		}
		ProductsEntity existing = productRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
		if (!companyId.equals(existing.getCompanyId())) {
			throw new IllegalArgumentException("Không tìm thấy sản phẩm");
		}
		productRepository.delete(existing);
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
