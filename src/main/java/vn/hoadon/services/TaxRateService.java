package vn.hoadon.services;

import vn.hoadon.entity.TaxRateEntity;

import java.util.List;

public interface TaxRateService {

    // Create
    TaxRateEntity create(TaxRateEntity taxRate);

    // Read - get all
    List<TaxRateEntity> findAll();

    // Read - get by id
    TaxRateEntity findById(Integer id);

    // Update
    TaxRateEntity update(Integer id, TaxRateEntity taxRate);

    // Delete
    void delete(Integer id);
}
