package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.DailyInvoiceReportConfigEntity;

import java.util.Optional;

public interface DailyInvoiceReportConfigRepository extends JpaRepository<DailyInvoiceReportConfigEntity, Integer> {
    Optional<DailyInvoiceReportConfigEntity> findTopByOrderByIdDesc();
}
