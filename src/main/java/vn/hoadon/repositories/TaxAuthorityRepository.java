package vn.hoadon.repositories;

import vn.hoadon.entity.TaxAuthorityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxAuthorityRepository extends JpaRepository<TaxAuthorityEntity, Long> {

    // Override findAll để tối ưu hóa việc lấy tên CQT Cha
    // @EntityGraph giúp fetch luôn thằng parent mà không cần query thêm (tránh N+1)
    @EntityGraph(attributePaths = {"parent"}) 
    Page<TaxAuthorityEntity> findAll(Specification<TaxAuthorityEntity> spec, Pageable pageable);
    List<TaxAuthorityEntity> findByStatus(Integer status);
    List<TaxAuthorityEntity> findByParentIdAndStatus(Long parentId, Integer status);
    List<TaxAuthorityEntity> findByParentId(Long parentId);
    Optional<TaxAuthorityEntity> findByCode(Integer code);

}