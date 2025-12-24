package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.hoadon.entity.UserPermissionEntity;

import java.util.List;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermissionEntity, Long> {
    List<UserPermissionEntity> findByUserId(Long userId);
    void deleteByUserIdAndPermissionId(Long userId, Long permissionId);
    UserPermissionEntity findByUserIdAndPermissionId(Long userId, Long permissionId);
    void deleteAllByUserId(Long userId);
}