package vn.hoadon.services;

import vn.hoadon.entity.TaxAuthorityEntity;
import java.util.List;
import java.util.Optional;

public interface TaxAuthorityService {
    List<TaxAuthorityEntity> listCities();
    List<TaxAuthorityEntity> listByParent(Long parentId);
    List<TaxAuthorityEntity> listByParentActive(Long parentId);
    Optional<TaxAuthorityEntity> findByCode(Integer code);
}