package vn.hoadon.services;


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
}
