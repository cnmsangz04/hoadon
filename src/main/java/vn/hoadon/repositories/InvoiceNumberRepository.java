package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.hoadon.entity.InvoiceNumberEntity;

import java.util.List;

@Repository
public interface InvoiceNumberRepository extends JpaRepository<InvoiceNumberEntity, Long> {
    List<InvoiceNumberEntity> findByCompanyIdAndFormId(Long companyId, Long formId);
    List<InvoiceNumberEntity> findByCompanyIdAndStatus(Long companyId, Integer status);
    List<InvoiceNumberEntity> findByCompanyIdAndCategoryAndStatus(Long companyId, Integer category, Integer status);
}