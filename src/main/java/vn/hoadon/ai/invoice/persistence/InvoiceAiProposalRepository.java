package vn.hoadon.ai.invoice.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceAiProposalRepository extends JpaRepository<InvoiceAiProposalEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from InvoiceAiProposalEntity p where p.id = :id and p.userId = :userId and p.companyId = :companyId")
    Optional<InvoiceAiProposalEntity> findOwnedForUpdate(@Param("id") Long id, @Param("userId") Long userId,
                                                         @Param("companyId") Long companyId);
}
