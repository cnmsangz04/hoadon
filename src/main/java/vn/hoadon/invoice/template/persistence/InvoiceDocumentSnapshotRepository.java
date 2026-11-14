package vn.hoadon.invoice.template.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InvoiceDocumentSnapshotRepository extends JpaRepository<InvoiceDocumentSnapshotEntity, Long> {
    Optional<InvoiceDocumentSnapshotEntity> findTopByInvoiceIdOrderByIdAsc(Long invoiceId);
    Optional<InvoiceDocumentSnapshotEntity> findTopByInvoiceIdAndTemplateVersionIdAndSourceStageAndSourceHashOrderByIdDesc(Long invoiceId, Long templateVersionId, String sourceStage, String sourceHash);
}
