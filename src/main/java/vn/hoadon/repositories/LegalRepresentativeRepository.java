package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.LegalRepresentativeEntity;

import java.util.Optional;

public interface LegalRepresentativeRepository extends JpaRepository<LegalRepresentativeEntity, Long> {
    Optional<LegalRepresentativeEntity> findByCompanyId(Long companyId);
}
