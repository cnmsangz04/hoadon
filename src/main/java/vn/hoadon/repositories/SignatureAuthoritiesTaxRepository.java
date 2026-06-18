package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.hoadon.entity.SignatureAuthoritiesTaxEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignatureAuthoritiesTaxRepository extends JpaRepository<SignatureAuthoritiesTaxEntity, Integer> {
    Optional<SignatureAuthoritiesTaxEntity> findTopByInvoiceIdOrderByIdDesc(Integer invoiceId);
    List<SignatureAuthoritiesTaxEntity> findByInvoiceId(Integer invoiceId);
    Optional<SignatureAuthoritiesTaxEntity> findTopByInvoiceIdAndCompanyIdOrderByIdDesc(Integer invoiceId, Integer companyId);
    List<SignatureAuthoritiesTaxEntity> findByInvoiceIdAndCompanyId(Integer invoiceId, Integer companyId);
}
