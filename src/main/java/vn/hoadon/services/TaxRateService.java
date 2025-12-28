package vn.hoadon.services;

import vn.hoadon.entity.TaxRate;

import java.util.List;

public interface TaxRateService {

    // Create
    TaxRate create(TaxRate taxRate);

    // Read - get all
    List<TaxRate> findAll();

    // Read - get by id
    TaxRate findById(Integer id);

    // Update
    TaxRate update(Integer id, TaxRate taxRate);

    // Delete
    void delete(Integer id);
}
