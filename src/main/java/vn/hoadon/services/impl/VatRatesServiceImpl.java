package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.hoadon.entity.VatRatesEntity;
import vn.hoadon.repositories.VaRatesRepository;
import vn.hoadon.services.VatRatesService;

import java.util.List;

@Service
public class VatRatesServiceImpl implements VatRatesService {

    @Autowired
    private VaRatesRepository vaRatesRepository;

    @Override
    public VatRatesEntity create(VatRatesEntity taxRate) {
        return vaRatesRepository.save(taxRate);
    }

    @Override
    public List<VatRatesEntity> findAll() {
        return vaRatesRepository.findAll();
    }

    @Override
    public VatRatesEntity findById(Integer id) {
        return vaRatesRepository.findById(id).orElse(null);
    }

    @Override
    public VatRatesEntity update(Integer id, VatRatesEntity taxRate) {
        VatRatesEntity existing = vaRatesRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }

        existing.setCode(taxRate.getCode());
        existing.setLabel(taxRate.getLabel());
        existing.setStatus(taxRate.getStatus());
        existing.setUpdatedAt(taxRate.getUpdatedAt());

        return vaRatesRepository.save(existing);
    }

    @Override
    public void delete(Integer id) {
        vaRatesRepository.deleteById(id);
    }
}
