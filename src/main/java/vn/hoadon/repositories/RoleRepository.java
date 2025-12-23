package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.hoadon.entity.RoleEntity;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long>, JpaSpecificationExecutor<RoleEntity> {

    Optional<RoleEntity> findByName(String name);

    // Ensure permissions are loaded for listing to display chips without N+1
    @Override
    @EntityGraph(attributePaths = {"permissions"})
    Page<RoleEntity> findAll(org.springframework.data.jpa.domain.Specification<RoleEntity> spec, Pageable pageable);
}