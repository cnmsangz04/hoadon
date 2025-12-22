package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.CompanyBankEntity;
import vn.hoadon.entity.CompanyEntity;

import java.util.List;

public interface CompanyBankRepository extends JpaRepository<CompanyBankEntity, Long> {
    List<CompanyBankEntity> findByCompany(CompanyEntity company);
}