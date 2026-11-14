package vn.hoadon.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import vn.hoadon.entity.CompanyEntity;

public interface CompanyRepository extends JpaRepository<CompanyEntity, Long>, JpaSpecificationExecutor<CompanyEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CompanyEntity c where c.id = :id")
    Optional<CompanyEntity> lockById(@Param("id") Long id);
	boolean existsByPrefix(String prefix);
	boolean existsByTaxcode(String taxcode);
	Optional<CompanyEntity> findByTaxcode(String taxcode);
}
