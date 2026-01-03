package vn.hoadon.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.hoadon.entity.CustomersEntity;

public interface CustomerRepository extends JpaRepository<CustomersEntity, Long>, JpaSpecificationExecutor<CustomersEntity>{
	List<CustomersEntity> findByCode(String code);
	List<CustomersEntity> findByStatus(Integer status);
}
