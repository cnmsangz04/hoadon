package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import vn.hoadon.entity.CompanyAllowedIpEntity;

public interface CompanyAllowedIpRepository extends JpaRepository<CompanyAllowedIpEntity, Long> {

    List<CompanyAllowedIpEntity> findByCompanyIdOrderByOriginalIpDescIdAsc(Long companyId);

    Optional<CompanyAllowedIpEntity> findFirstByCompanyIdAndIpAddressIgnoreCase(Long companyId, String ipAddress);

    Optional<CompanyAllowedIpEntity> findFirstByCompanyIdAndOriginalIpTrue(Long companyId);

    boolean existsByCompanyIdAndIpAddressIgnoreCaseAndStatus(Long companyId, String ipAddress, Integer status);
}
