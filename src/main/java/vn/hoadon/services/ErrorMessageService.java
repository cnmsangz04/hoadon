package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.hoadon.dto.errormessage.ErrorMessageDto;
import vn.hoadon.dto.errormessage.ErrorMessageUpsertRequest;
import vn.hoadon.entity.ErrorMessageEntity;

import java.time.LocalDate;
import java.util.Optional;

public interface ErrorMessageService {
    Page<ErrorMessageEntity> search(Long companyId, String keyword, Integer status, Integer notificationType, LocalDate dateFrom, LocalDate dateTo, Pageable pageable);
    Optional<ErrorMessageEntity> findById(Long id);
    Optional<ErrorMessageEntity> findByIdAndCompany(Long id, Long companyId);
    ErrorMessageEntity create(Long companyId, Long userId, ErrorMessageUpsertRequest req);
    Optional<ErrorMessageEntity> update(Long companyId, Long id, ErrorMessageUpsertRequest req);
    Optional<ErrorMessageEntity> attachSignedXml(Long id, String signedXml, String signatureInfo);
    Optional<ErrorMessageEntity> send(Long companyId, Long userId, Long id);
    void delete(Long companyId, Long id);
    String buildUnsignedXml(ErrorMessageEntity entity);
    Optional<String> getXmlForDownload(Long id);
    ErrorMessageDto toDto(ErrorMessageEntity entity);
}
