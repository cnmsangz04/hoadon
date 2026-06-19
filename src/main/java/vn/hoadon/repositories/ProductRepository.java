package vn.hoadon.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.hoadon.entity.ProductsEntity;

public interface ProductRepository extends JpaRepository<ProductsEntity, Long>, JpaSpecificationExecutor<ProductsEntity>{
	List<ProductsEntity> findByName(String name);
	List<ProductsEntity> findByCode(String code);
	List<ProductsEntity> findByCompanyIdAndCode(Long companyId, String code);
	List<ProductsEntity> findByStatus(Integer status);
	
}
