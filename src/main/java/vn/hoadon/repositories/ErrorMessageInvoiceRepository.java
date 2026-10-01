package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.ErrorMessageInvoiceEntity;

import java.util.List;

public interface ErrorMessageInvoiceRepository extends JpaRepository<ErrorMessageInvoiceEntity, Long> {
    List<ErrorMessageInvoiceEntity> findByErrorMessage_IdOrderByLineNoAscIdAsc(Long errorMessageId);
}
