package vn.hoadon.services;

import java.util.*;

import org.springframework.data.domain.*;

import vn.hoadon.dto.product.ProductFilterDTO;
import vn.hoadon.entity.ProductsEntity;

public interface ProductService {
	Page<ProductsEntity> list(ProductFilterDTO filter, Pageable pageable);
	List<ProductsEntity> list(Integer status);
	ProductsEntity saveOrUpdate(ProductsEntity product);
	void setLock(Long id, boolean lock);
	Optional<ProductsEntity> finbyCode(String code);
	Optional<ProductsEntity> findbyName(String name);
	

}
