package vn.hoadon.services.impl;

import org.springframework.stereotype.Service;
import vn.hoadon.entity.ProvinceEntity;
import vn.hoadon.repositories.ProvinceRepository;
import vn.hoadon.services.ProvinceService;

import java.util.List;

@Service
public class ProvinceServiceImpl implements ProvinceService {

    private final ProvinceRepository provinceRepository;

    public ProvinceServiceImpl(ProvinceRepository provinceRepository) {
        this.provinceRepository = provinceRepository;
    }

    @Override
    public List<ProvinceEntity> getActiveProvinces() {
        return provinceRepository.findByStatus(1);
    }
}
