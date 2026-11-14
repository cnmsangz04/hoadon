package vn.hoadon.invoice.template.persistence;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface InvoiceTemplateFieldRepository
        extends JpaRepository<InvoiceTemplateFieldEntity, Long>,
                JpaSpecificationExecutor<InvoiceTemplateFieldEntity> {
    List<InvoiceTemplateFieldEntity> findAllByOrderBySortOrderAscIdAsc();

    boolean existsByFieldKey(String fieldKey);

    @Query("select coalesce(max(f.sortOrder), 0) from InvoiceTemplateFieldEntity f")
    int maximumSortOrder();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from InvoiceTemplateFieldEntity f where f.id = :id")
    Optional<InvoiceTemplateFieldEntity> lockById(@Param("id") Long id);
}
