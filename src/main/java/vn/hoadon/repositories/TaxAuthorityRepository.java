package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.TaxAuthorityEntity;
import java.util.List;
import java.util.Optional;

public interface TaxAuthorityRepository extends JpaRepository<TaxAuthorityEntity, Long> {
    List<TaxAuthorityEntity> findByStatus(Integer status);
    List<TaxAuthorityEntity> findByParentIdAndStatus(Long parentId, Integer status);
    List<TaxAuthorityEntity> findByParentId(Long parentId);
    Optional<TaxAuthorityEntity> findByCode(Integer code);
}