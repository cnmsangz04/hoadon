package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.VatRatesEntity;

public interface VaRatesRepository extends JpaRepository<VatRatesEntity, Integer> {
}
