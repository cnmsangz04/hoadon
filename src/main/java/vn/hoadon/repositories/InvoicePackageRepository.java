package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.hoadon.entity.InvoicePackageEntity;

import java.util.List;

public interface InvoicePackageRepository extends JpaRepository<InvoicePackageEntity, Long>,
        JpaSpecificationExecutor<InvoicePackageEntity> {

    List<InvoicePackageEntity> findByStatusOrderByDisplayOrderAscIdAsc(Integer status);
}
