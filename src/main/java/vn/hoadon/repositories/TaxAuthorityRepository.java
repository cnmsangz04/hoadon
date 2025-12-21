package vn.hoadon.repositories;

import vn.hoadon.model.TaxAuthority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxAuthorityRepository extends JpaRepository<TaxAuthority, Long> {

    // Override findAll để tối ưu hóa việc lấy tên CQT Cha
    // @EntityGraph giúp fetch luôn thằng parent mà không cần query thêm (tránh N+1)
    @EntityGraph(attributePaths = {"parent"}) 
    Page<TaxAuthority> findAll(Specification<TaxAuthority> spec, Pageable pageable);
}