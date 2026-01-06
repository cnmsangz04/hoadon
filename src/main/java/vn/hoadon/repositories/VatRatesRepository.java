package vn.hoadon.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.hoadon.entity.VatRatesEntity;

@Repository
public interface VatRatesRepository extends JpaRepository<VatRatesEntity, Integer> {
    Page<VatRatesEntity> findByUserId(Integer userId, Pageable pageable);
    Page<VatRatesEntity> findByUserIdAndStatus(Integer userId, Integer status, Pageable pageable);
    List<VatRatesEntity> findAllByOrderByPrioritizeAsc();
}
