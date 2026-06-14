package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.hoadon.entity.CompanyRegistrationRequestEntity;

public interface CompanyRegistrationRequestRepository
        extends JpaRepository<CompanyRegistrationRequestEntity, Long>,
        JpaSpecificationExecutor<CompanyRegistrationRequestEntity> {

    boolean existsByTaxcodeAndStatus(String taxcode, Integer status);
}
