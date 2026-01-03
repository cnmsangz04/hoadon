package vn.hoadon.services;

import java.util.List;

import org.springframework.data.domain.*;

import vn.hoadon.dto.customer.CustomerFilterDTO;
import vn.hoadon.entity.CustomersEntity;

public interface CustomerService {
	Page<CustomersEntity> list(CustomerFilterDTO filter, Pageable pageable);
	List<CustomersEntity> list(Integer status);
    CustomersEntity saveOrUpdate(CustomersEntity customer);
}
