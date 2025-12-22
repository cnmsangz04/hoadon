package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.hoadon.entity.PermissionCategoryEntity;

import java.util.Optional;

public interface PermissionCategoryService {
    Page<PermissionCategoryEntity> list(String keyword, Pageable pageable);
    Optional<PermissionCategoryEntity> findById(Long id);
    PermissionCategoryEntity saveOrUpdate(PermissionCategoryEntity entity);
    void delete(Long id);
}
