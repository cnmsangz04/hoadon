package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.TaxRate;

public interface TaxRateRepository extends JpaRepository<TaxRate, Integer> {
}
