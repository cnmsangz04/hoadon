package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.TaxRateEntity;

public interface TaxRateRepository extends JpaRepository<TaxRateEntity, Integer> {
}
