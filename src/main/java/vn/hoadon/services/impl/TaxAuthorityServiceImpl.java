package vn.hoadon.services.impl;

import org.springframework.stereotype.Service;
import vn.hoadon.entity.TaxAuthorityEntity;
import vn.hoadon.repositories.TaxAuthorityRepository;
import vn.hoadon.services.TaxAuthorityService;

import java.util.List;
import java.util.Optional;

@Service
public class TaxAuthorityServiceImpl implements TaxAuthorityService {

    private final TaxAuthorityRepository repo;

    public TaxAuthorityServiceImpl(TaxAuthorityRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<TaxAuthorityEntity> listCities() {
        return repo.findByParentIdAndStatus(0L, 1);
    }

    @Override
    public List<TaxAuthorityEntity> listByParent(Long parentId) {
        return repo.findByParentId(parentId);
    }

    @Override
    public List<TaxAuthorityEntity> listByParentActive(Long parentId) {
        return repo.findByParentIdAndStatus(parentId, 1);
    }

    @Override
    public Optional<TaxAuthorityEntity> findByCode(Integer code) {
        return repo.findByCode(code);
    }
}