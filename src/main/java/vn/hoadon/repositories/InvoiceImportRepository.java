package vn.hoadon.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hoadon.entity.InvoiceImportEntity;

import java.util.Optional;

public interface InvoiceImportRepository extends JpaRepository<InvoiceImportEntity, Long> {
    Page<InvoiceImportEntity> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);
    Optional<InvoiceImportEntity> findByIdAndCompanyId(Long id, Long companyId);
    Page<InvoiceImportEntity> findByCompanyIdAndImportTypeOrderByIdDesc(Long companyId, String importType, Pageable pageable);
    Optional<InvoiceImportEntity> findByIdAndCompanyIdAndImportType(Long id, Long companyId, String importType);

    @Query("select e from InvoiceImportEntity e where e.companyId = :companyId and (e.importType = 'INVOICE' or e.importType is null) order by e.id desc")
    Page<InvoiceImportEntity> findInvoiceImportsByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("select e from InvoiceImportEntity e where e.id = :id and e.companyId = :companyId and (e.importType = 'INVOICE' or e.importType is null)")
    Optional<InvoiceImportEntity> findInvoiceImportByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
