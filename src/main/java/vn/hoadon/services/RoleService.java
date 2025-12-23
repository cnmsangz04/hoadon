package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.hoadon.entity.RoleEntity;

import java.util.Optional;

public interface RoleService {
    Page<RoleEntity> list(String keyword, Pageable pageable);
    Optional<RoleEntity> findById(Long id);
    RoleEntity saveOrUpdate(RoleEntity entity);
    void delete(Long id);
    // Clone a role with a new name/displayName and copy its permissions
    RoleEntity cloneRole(Long id, String name, String displayName);
}