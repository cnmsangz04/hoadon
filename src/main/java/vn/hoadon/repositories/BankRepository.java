package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.hoadon.entity.BankEntity;

import java.util.List;
import java.util.Optional;

public interface BankRepository extends JpaRepository<BankEntity, Long>, JpaSpecificationExecutor<BankEntity>  {
    List<BankEntity> findByStatus(Integer status);
    List<BankEntity> findByAbbreviation(String abbreviation);
    List<BankEntity> findByName(String name);
}