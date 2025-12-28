package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.hoadon.entity.TaxRate;
import vn.hoadon.repositories.TaxRateRepository;
import vn.hoadon.services.TaxRateService;

import java.util.List;

@Service
public class TaxRateServiceImpl implements TaxRateService {

    @Autowired
    private TaxRateRepository taxRateRepository;

    @Override
    public TaxRate create(TaxRate taxRate) {
        return taxRateRepository.save(taxRate);
    }

    @Override
    public List<TaxRate> findAll() {
        return taxRateRepository.findAll();
    }

    @Override
    public TaxRate findById(Integer id) {
        return taxRateRepository.findById(id).orElse(null);
    }

    @Override
    public TaxRate update(Integer id, TaxRate taxRate) {
        TaxRate existing = taxRateRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }

        existing.setRate(taxRate.getRate());
        existing.setStatus(taxRate.getStatus());

        return taxRateRepository.save(existing);
    }

    @Override
    public void delete(Integer id) {
        taxRateRepository.deleteById(id);
    }
}
