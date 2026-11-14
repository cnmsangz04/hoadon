package vn.hoadon.invoice.template.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRenderJobRepository extends JpaRepository<InvoiceRenderJobEntity, Long> {
    Optional<InvoiceRenderJobEntity> findByRequestKey(String requestKey);
    Optional<InvoiceRenderJobEntity> findTopBySnapshotIdAndTemplateVersionIdAndPurposeAndStateOrderByIdDesc(Long snapshotId, Long templateVersionId, String purpose, String state);
    @Query("select j.id from InvoiceRenderJobEntity j where j.state = 'QUEUED' or (j.state = 'RUNNING' and j.leaseUntil < :now) order by j.id")
    List<Long> findAvailable(@Param("now") LocalDateTime now, Pageable pageable);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update InvoiceRenderJobEntity j set j.state = 'RUNNING', j.claimToken = :token, j.leaseUntil = :lease, j.attemptCount = j.attemptCount + 1, j.lockVersion = j.lockVersion + 1 where j.id = :id and (j.state = 'QUEUED' or (j.state = 'RUNNING' and j.leaseUntil < :now))")
    int claim(@Param("id") Long id, @Param("token") String token, @Param("lease") LocalDateTime lease, @Param("now") LocalDateTime now);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from InvoiceRenderJobEntity j where j.id = :id")
    Optional<InvoiceRenderJobEntity> lockById(@Param("id") Long id);
}
