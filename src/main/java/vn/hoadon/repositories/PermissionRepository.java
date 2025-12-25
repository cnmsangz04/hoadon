package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.hoadon.entity.PermissionEntity;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long>, JpaSpecificationExecutor<PermissionEntity> {

    Optional<PermissionEntity> findByName(String name);

    List<PermissionEntity> findByLevelAndStatus(Integer level, Byte status);
}