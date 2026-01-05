package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.hoadon.entity.SignatureVatEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignatureVatRepository extends JpaRepository<SignatureVatEntity, Integer> {
    Optional<SignatureVatEntity> findTopByInvoiceIdOrderByIdDesc(Integer invoiceId);
    List<SignatureVatEntity> findByInvoiceId(Integer invoiceId);
}
