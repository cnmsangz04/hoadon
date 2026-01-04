package vn.hoadon.services;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.hoadon.entity.VatRatesEntity;

import java.util.List;

public interface VatRatesService {

    // Create
    VatRatesEntity create(VatRatesEntity taxRate);

    // Read - get all
    List<VatRatesEntity> findAll();

    // Read - get by id
    VatRatesEntity findById(Integer id);

    // Update
    VatRatesEntity update(Integer id, VatRatesEntity taxRate);

    // Delete
    void delete(Integer id);

    // Pagination by user
    Page<VatRatesEntity> pageByUser(Integer userId, Integer status, Pageable pageable, String keyword);
}