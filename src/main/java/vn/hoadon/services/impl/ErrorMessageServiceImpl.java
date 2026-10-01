package vn.hoadon.services.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.dto.errormessage.*;
import vn.hoadon.entity.*;
import vn.hoadon.repositories.*;
import vn.hoadon.services.ErrorMessageService;
import vn.hoadon.services.patvan.PatvanMessageType;
import vn.hoadon.services.patvan.PatvanPostResult;
import vn.hoadon.services.patvan.PatvanTransmissionService;
import vn.hoadon.util.ErrorMessageXmlBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ErrorMessageServiceImpl implements ErrorMessageService {
    private final ErrorMessageRepository repository;
    private final InvoiceRepository invoiceRepository;
    private final FormInvoiceRepository formInvoiceRepository;
    private final CompanyRepository companyRepository;
    private final ProvinceRepository provinceRepository;
    private final HistoryRepository historyRepository;
    private final PatvanTransmissionService patvanTransmissionService;

    public ErrorMessageServiceImpl(ErrorMessageRepository repository,
                                   InvoiceRepository invoiceRepository,
                                   FormInvoiceRepository formInvoiceRepository,
                                   CompanyRepository companyRepository,
                                   ProvinceRepository provinceRepository,
                                   HistoryRepository historyRepository,
                                   PatvanTransmissionService patvanTransmissionService) {
        this.repository = repository;
        this.invoiceRepository = invoiceRepository;
        this.formInvoiceRepository = formInvoiceRepository;
        this.companyRepository = companyRepository;
        this.provinceRepository = provinceRepository;
        this.historyRepository = historyRepository;
        this.patvanTransmissionService = patvanTransmissionService;
    }

    @Override
    public Page<ErrorMessageEntity> search(Long companyId, String keyword, Integer status, Integer notificationType, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        return repository.search(companyId, keyword, status, notificationType, dateFrom, dateTo, pageable);
    }

    @Override
    public Optional<ErrorMessageEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<ErrorMessageEntity> findByIdAndCompany(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId);
    }

    @Override
    @Transactional
    public ErrorMessageEntity create(Long companyId, Long userId, ErrorMessageUpsertRequest req) {
        ErrorMessageEntity entity = new ErrorMessageEntity();
        entity.setCompanyId(companyId);
        entity.setUserId(userId);
        entity.setIdAttr(UUID.randomUUID().toString().replace("-", "").toUpperCase());
        applyHeader(entity, companyId, req);
        replaceLines(entity, companyId, req.getInvoices());
        return repository.save(entity);
    }

    @Override
    @Transactional
    public Optional<ErrorMessageEntity> update(Long companyId, Long id, ErrorMessageUpsertRequest req) {
        return repository.findByIdAndCompanyId(id, companyId).map(existing -> {
            if (existing.getStatus() != null && existing.getStatus() != 0) {
                throw new IllegalArgumentException("Chỉ được cập nhật thông báo mới khởi tạo");
            }
            applyHeader(existing, companyId, req);
            existing.setSignedXml(null);
            existing.setSignatureInfo(null);
            existing.setSignDate(null);
            existing.setStatus(0);
            replaceLines(existing, companyId, req.getInvoices());
            return repository.save(existing);
        });
    }

    @Override
    @Transactional
    public Optional<ErrorMessageEntity> attachSignedXml(Long id, String signedXml, String signatureInfo) {
        return repository.findById(id).map(existing -> {
            existing.setSignedXml(signedXml);
            existing.setSignatureInfo(signatureInfo);
            existing.setSignDate(LocalDateTime.now());
            existing.setStatus(1);
            return repository.save(existing);
        });
    }

    @Override
    @Transactional
    public Optional<ErrorMessageEntity> send(Long companyId, Long userId, Long id) {
        return repository.findByIdAndCompanyId(id, companyId).map(existing -> {
            if (existing.getSignedXml() == null || existing.getSignedXml().isBlank()) {
                throw new IllegalArgumentException("Thông báo chưa được ký số");
            }
            CompanyEntity company = companyRepository.findById(companyId).orElse(null);
            String taxCode = company != null ? company.getTaxcode() : null;
            PatvanMessageType type = isCertificateErrorMessage(existing) ? PatvanMessageType.TBSS_CT : PatvanMessageType.TBSS;
            PatvanPostResult patvanResult = patvanTransmissionService.postXml(type, existing.getSignedXml(), taxCode);
            existing.setMessageCode(patvanResult.maThongdiep());
            existing.setStatus(2);
            ErrorMessageEntity saved = repository.save(existing);
            saveHistory(companyId, userId, id, 300, "Gửi thông báo hóa đơn sai sót lên cơ quan thuế", "Mã thông điệp 300", existing.getSignedXml(), 0);
            return saved;
        });
    }

    @Override
    @Transactional
    public void delete(Long companyId, Long id) {
        ErrorMessageEntity entity = repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thông báo sai sót"));
        if (entity.getStatus() != null && entity.getStatus() != 0) {
            throw new IllegalArgumentException("Chỉ được xóa thông báo mới khởi tạo");
        }
        repository.delete(entity);
    }

    @Override
    public String buildUnsignedXml(ErrorMessageEntity entity) {
        return ErrorMessageXmlBuilder.buildUnsigned(entity);
    }

    @Override
    public Optional<String> getXmlForDownload(Long id) {
        return repository.findById(id).map(e -> e.getSignedXml() != null && !e.getSignedXml().isBlank()
                ? e.getSignedXml()
                : buildUnsignedXml(e));
    }

    @Override
    public ErrorMessageDto toDto(ErrorMessageEntity entity) {
        if (entity == null) return null;
        ErrorMessageDto dto = new ErrorMessageDto();
        dto.setId(entity.getId());
        dto.setIdAttr(entity.getIdAttr());
        dto.setMessageCode(entity.getMessageCode());
        dto.setTaxNoticeNumber(entity.getTaxNoticeNumber());
        dto.setTaxResponseNumber(entity.getTaxResponseNumber());
        dto.setFormPattern(entity.getFormPattern());
        dto.setMessageType(entity.getMessageType());
        dto.setNotificationType(entity.getNotificationType());
        dto.setTaxAuthorityCode(entity.getTaxAuthorityCode());
        dto.setTaxAuthorityName(entity.getTaxAuthorityName());
        dto.setTaxCode(entity.getTaxCode());
        dto.setTaxpayerName(entity.getTaxpayerName());
        dto.setCreatePlace(entity.getCreatePlace());
        dto.setNoticeDate(entity.getNoticeDate());
        dto.setTaxNoticeDate(entity.getTaxNoticeDate());
        dto.setDocumentType(entity.getDocumentType());
        dto.setSignedXml(entity.getSignedXml());
        dto.setSignatureInfo(entity.getSignatureInfo());
        dto.setSignDate(entity.getSignDate());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        List<ErrorMessageInvoiceDto> lines = new ArrayList<>();
        if (entity.getInvoices() != null) {
            for (ErrorMessageInvoiceEntity line : entity.getInvoices()) {
                ErrorMessageInvoiceDto l = new ErrorMessageInvoiceDto();
                l.setId(line.getId());
                l.setInvoiceId(line.getInvoiceId());
                l.setLineNo(line.getLineNo());
                l.setTaxCode(line.getTaxCode());
                l.setFormSymbol(line.getFormSymbol());
                l.setSerial(line.getSerial());
                l.setInvoiceNo(line.getInvoiceNo());
                l.setInvoiceDate(line.getInvoiceDate());
                l.setInvoiceType(line.getInvoiceType());
                l.setErrorType(line.getErrorType());
                l.setReason(line.getReason());
                lines.add(l);
            }
        }
        dto.setInvoices(lines);
        return dto;
    }

    private void applyHeader(ErrorMessageEntity entity, Long companyId, ErrorMessageUpsertRequest req) {
        CompanyEntity company = companyRepository.findById(companyId).orElse(null);
        entity.setFormPattern(nonBlank(req.getFormPattern(), "04/SS-HĐĐT"));
        entity.setMessageType("TBSS");
        entity.setNotificationType(req.getNotificationType() != null && req.getNotificationType() == 2 ? 2 : 1);
        entity.setTaxNoticeNumber(entity.getNotificationType() == 2 ? trim(req.getTaxNoticeNumber()) : null);
        entity.setTaxNoticeDate(entity.getNotificationType() == 2 ? parseDate(req.getTaxNoticeDate(), null) : null);
        entity.setCreatePlace(resolveCreatePlaceName(req.getCreatePlace()));
        entity.setNoticeDate(parseDate(req.getNoticeDate(), LocalDate.now()));
        entity.setDocumentType(nonBlank(req.getDocumentType(), "INVOICE"));
        if (company != null) {
            entity.setTaxCode(company.getTaxcode());
            entity.setTaxpayerName(company.getName());
            TaxAuthorityEntity taxAuthority = company.getTaxAuthority() != null ? company.getTaxAuthority() : company.getTaxAuthorityCity();
            if (taxAuthority != null) {
                entity.setTaxAuthorityCode(taxAuthority.getCode() != null ? String.valueOf(taxAuthority.getCode()) : "");
                entity.setTaxAuthorityName(taxAuthority.getName());
            }
        }
    }

    private void replaceLines(ErrorMessageEntity entity, Long companyId, List<ErrorMessageInvoiceRequest> rows) {
        entity.getInvoices().clear();
        int lineNo = 1;
        if (rows == null) rows = List.of();
        for (ErrorMessageInvoiceRequest req : rows) {
            ErrorMessageInvoiceEntity line = buildLine(companyId, req);
            if (line == null) continue;
            line.setErrorMessage(entity);
            line.setLineNo(lineNo++);
            entity.getInvoices().add(line);
        }
        if (entity.getInvoices().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một hóa đơn sai sót");
        }
    }

    private ErrorMessageInvoiceEntity buildLine(Long companyId, ErrorMessageInvoiceRequest req) {
        if (req == null) return null;
        InvoiceEntity invoice = null;
        if (req.getInvoiceId() != null) {
            invoice = invoiceRepository.findById(req.getInvoiceId()).orElse(null);
            if (invoice == null || invoice.getCompanyId() == null || !companyId.equals(invoice.getCompanyId().longValue())) {
                throw new IllegalArgumentException("Không tìm thấy hóa đơn #" + req.getInvoiceId());
            }
        }
        ErrorMessageInvoiceEntity line = new ErrorMessageInvoiceEntity();
        line.setInvoiceId(invoice != null ? invoice.getId() : req.getInvoiceId());
        if (invoice != null) {
            FormInvoiceEntity form = invoice.getFormId() != null ? formInvoiceRepository.findById(invoice.getFormId().longValue()).orElse(null) : null;
            line.setTaxCode(nonBlank(req.getTaxCode(), invoice.getCodeCqt()));
            line.setFormSymbol(nonBlank(req.getFormSymbol(), form != null ? form.getFormCode() : firstSerialChar(form != null ? form.getSerial() : "")));
            line.setSerial(nonBlank(req.getSerial(), form != null ? form.getSerial() : ""));
            line.setInvoiceNo(nonBlank(req.getInvoiceNo(), invoice.getNo() != null ? String.valueOf(invoice.getNo()) : ""));
            line.setInvoiceDate(parseDate(req.getInvoiceDate(), invoice.getDateExport()));
            line.setInvoiceType(req.getInvoiceType() != null ? req.getInvoiceType() : 1);
        } else {
            line.setTaxCode(trim(req.getTaxCode()));
            line.setFormSymbol(trim(req.getFormSymbol()));
            line.setSerial(trim(req.getSerial()));
            line.setInvoiceNo(trim(req.getInvoiceNo()));
            line.setInvoiceDate(parseDate(req.getInvoiceDate(), null));
            line.setInvoiceType(req.getInvoiceType() != null ? req.getInvoiceType() : 1);
        }
        line.setErrorType(req.getErrorType() != null ? req.getErrorType() : 0);
        line.setReason(trim(req.getReason()));
        if (line.getInvoiceNo() == null || line.getInvoiceNo().isBlank() || line.getInvoiceDate() == null) {
            throw new IllegalArgumentException("Dòng hóa đơn sai sót thiếu số hoặc ngày hóa đơn");
        }
        return line;
    }

    private boolean isCertificateErrorMessage(ErrorMessageEntity entity) {
        String documentType = entity != null ? entity.getDocumentType() : null;
        if (documentType == null) return false;
        String normalized = documentType.trim().toUpperCase(Locale.ROOT);
        return normalized.contains("CT") || normalized.contains("CHUNG_TU") || normalized.contains("CHỨNG");
    }

    private void saveHistory(Long companyId, Long userId, Long tableId, Integer type, String title, String description, String xml, Integer showNotify) {
        HistoryEntity h = new HistoryEntity();
        h.setCompanyId(companyId);
        h.setUserId(userId);
        h.setTableName("error_messages");
        h.setTableId(tableId);
        h.setType(type);
        h.setTitle(title);
        h.setDescription(description);
        h.setXmlData(xml);
        h.setShowNotify(showNotify);
        h.setStatus(1);
        historyRepository.save(h);
    }

    private String resolveCreatePlaceName(String createPlace) {
        String value = trim(createPlace);
        if (value == null || value.isBlank() || !value.matches("\\d+")) return value;
        try {
            return provinceRepository.findById(Integer.valueOf(value))
                    .map(p -> p.getName() != null ? p.getName() : value)
                    .orElse(value);
        } catch (Exception e) {
            return value;
        }
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { return LocalDate.parse(value); } catch (Exception e) { return fallback; }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String nonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    private String firstSerialChar(String serial) {
        return serial != null && !serial.isBlank() ? serial.substring(0, 1) : "";
    }
}
