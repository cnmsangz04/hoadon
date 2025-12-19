package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.hoadon.entity.RoleEntity;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long>, JpaSpecificationExecutor<RoleEntity> {

    Optional<RoleEntity> findByName(String name);
}
