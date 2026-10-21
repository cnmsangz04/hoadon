package vn.hoadon.services.patvan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import vn.hoadon.config.AppEnvironment;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.ErrorMessageEntity;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.HistoryEntity;
import vn.hoadon.entity.InvoiceEntity;
import vn.hoadon.entity.RegisterInvoiceEntity;
import vn.hoadon.entity.SignatureAuthoritiesTaxEntity;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.ErrorMessageRepository;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.HistoryRepository;
import vn.hoadon.repositories.InvoiceRepository;
import vn.hoadon.repositories.RegisterInvoiceRepository;
import vn.hoadon.repositories.SignatureAuthoritiesTaxRepository;
import vn.hoadon.util.InvoiceXmlTaxValidator;
import vn.hoadon.util.RegisterInvoiceXmlTaxValidator;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Xử lý thông điệp CQT do PATVAN chuyển tiếp về hệ thống hóa đơn. */
@Service
public class PatvanCallbackService {
    private static final Logger log = LoggerFactory.getLogger(PatvanCallbackService.class);

    private static final String TYPE_PATH = "string((//*[local-name()='TTChung']/*[local-name()='MLTDiep'])[1])";
    private static final String REFERENCE_PATH = "string((//*[local-name()='TTChung']/*[local-name()='MTDTChieu'])[1])";
    private static final String NOTICE_PATH = "(//*[local-name()='DLTBao'])[1]";
    private static final Pattern XML_DECLARATION = Pattern.compile("(?is)^\\s*<\\?xml[^?]*\\?>");
    private static final Pattern HDON_CLOSE = Pattern.compile("(?is)</(?:[A-Za-z_][\\w.-]*:)?HDon>\\s*$");
    private static final long LOCAL_CALLBACK_DELAY_MS = 1500L;

    private final InvoiceRepository invoiceRepository;
    private final RegisterInvoiceRepository registerInvoiceRepository;
    private final ErrorMessageRepository errorMessageRepository;
    private final CompanyRepository companyRepository;
    private final FormInvoiceRepository formInvoiceRepository;
    private final HistoryRepository historyRepository;
    private final SignatureAuthoritiesTaxRepository signatureAuthoritiesTaxRepository;
    private final AppEnvironment appEnvironment;
    private final String providerTaxCode;

    public PatvanCallbackService(InvoiceRepository invoiceRepository,
                                 RegisterInvoiceRepository registerInvoiceRepository,
                                 ErrorMessageRepository errorMessageRepository,
                                 CompanyRepository companyRepository,
                                 FormInvoiceRepository formInvoiceRepository,
                                 HistoryRepository historyRepository,
                                 SignatureAuthoritiesTaxRepository signatureAuthoritiesTaxRepository,
                                 AppEnvironment appEnvironment,
                                 @Value("${patvan.callback.provider-tax-code:0302431595}") String providerTaxCode) {
        this.invoiceRepository = invoiceRepository;
        this.registerInvoiceRepository = registerInvoiceRepository;
        this.errorMessageRepository = errorMessageRepository;
        this.companyRepository = companyRepository;
        this.formInvoiceRepository = formInvoiceRepository;
        this.historyRepository = historyRepository;
        this.signatureAuthoritiesTaxRepository = signatureAuthoritiesTaxRepository;
        this.appEnvironment = appEnvironment;
        this.providerTaxCode = providerTaxCode;
        log.info("PATVAN callback mode: {} (app.backend-url={})",
                appEnvironment.isLocal() ? "LOCAL_SIMULATION" : "REAL_CALLBACK",
                appEnvironment.getBackendUrl());
    }

    public boolean isLocalSimulation() {
        return appEnvironment.isLocal();
    }

    @Async
    @Transactional
    public void simulateInvoiceResult(String messageReference, String taxpayerTaxCode,
                                      boolean haveCode, String signedInvoiceXml) {
        if (!appEnvironment.isLocal()) return;
        try {
            Thread.sleep(LOCAL_CALLBACK_DELAY_MS);
            InvoiceXmlTaxValidator.Result validation = InvoiceXmlTaxValidator.validate(signedInvoiceXml);
            String callbackXml = validation.isValid()
                    ? acceptedCallback(messageReference, haveCode, signedInvoiceXml)
                    : rejectedCallback(messageReference, haveCode, validation.getMessage());
            Result result = process(providerTaxCode, taxpayerTaxCode, callbackXml);
            log.info("Local PATVAN callback simulated: reference={}, type={}, companyId={}",
                    result.messageReference(), result.messageType(), result.companyId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Local PATVAN callback simulation interrupted: reference={}", messageReference);
        } catch (Exception e) {
            log.error("Local PATVAN callback simulation failed: reference={}, error={}",
                    messageReference, e.getMessage(), e);
        }
    }

    @Async
    @Transactional
    public void simulateRegisterResult(String messageReference, String taxpayerTaxCode, String signedXml) {
        if (!appEnvironment.isLocal()) return;
        try {
            Thread.sleep(1500L);
            RegisterInvoiceXmlTaxValidator.Result receive =
                    RegisterInvoiceXmlTaxValidator.validateForReceive(signedXml);
            process(providerTaxCode, taxpayerTaxCode,
                    registerCallbackXml(102, messageReference, receive.isValid(), receive.getMessage()));
            if (!receive.isValid()) return;

            Thread.sleep(2500L);
            RegisterInvoiceXmlTaxValidator.Result accepted = RegisterInvoiceXmlTaxValidator.validate(signedXml);
            process(providerTaxCode, taxpayerTaxCode,
                    registerCallbackXml(103, messageReference, accepted.isValid(), accepted.getMessage()));
            log.info("Local PATVAN register callback simulated: reference={}, accepted={}",
                    messageReference, accepted.isValid());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Local PATVAN register callback simulation interrupted: reference={}", messageReference);
        } catch (Exception e) {
            log.error("Local PATVAN register callback simulation failed: reference={}, error={}",
                    messageReference, e.getMessage(), e);
        }
    }

    @Async
    @Transactional
    public void simulateErrorMessageResult(String messageReference, String taxpayerTaxCode) {
        if (!appEnvironment.isLocal()) return;
        try {
            Thread.sleep(1500L);
            process(providerTaxCode, taxpayerTaxCode, errorMessageCallbackXml(messageReference));
            log.info("Local PATVAN error-message callback simulated: reference={}", messageReference);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Local PATVAN error-message callback simulation interrupted: reference={}", messageReference);
        } catch (Exception e) {
            log.error("Local PATVAN error-message callback simulation failed: reference={}, error={}",
                    messageReference, e.getMessage(), e);
        }
    }

    @Transactional
    public Result process(String providerTaxCode, String taxpayerTaxCode, String xml) {
        requireText(providerTaxCode, "Thiếu mst của đơn vị truyền nhận");
        requireText(xml, "Thiếu XML callback");

        Document document = parse(xml);
        int messageType = parseMessageType(text(document, TYPE_PATH));
        String reference = text(document, REFERENCE_PATH);
        requireText(reference, "Thông điệp callback thiếu MTDTChieu");

        CompanyEntity company = resolveCompany(reference, taxpayerTaxCode);
        long companyId = company.getId();

        switch (messageType) {
            case 102, 110 -> processRegisterReceive(document, xml, reference, companyId, messageType);
            case 103, 111 -> processRegisterAccept(document, xml, reference, companyId, messageType);
            case 202 -> processInvoiceAccepted(document, xml, reference, companyId, messageType);
            case 204, 213 -> processNotice(document, xml, reference, companyId, messageType);
            case 301 -> processErrorMessage301(document, xml, reference, companyId, messageType);
            case -1 -> processGeneralError(document, xml, reference, companyId, messageType);
            default -> log.info("PATVAN callback type {} is acknowledged without local mutation, reference={}",
                    messageType, reference);
        }

        return new Result(200, "success", messageType, reference, companyId);
    }

    private void processInvoiceAccepted(Document document, String xml, String reference,
                                        long companyId, int messageType) {
        InvoiceEntity invoice = findInvoice(reference, companyId);
        String cqtCode = firstText(document,
                "string((//*[local-name()='DLieu']/*[local-name()='HDon']/*[local-name()='MCCQT'])[1])",
                "string((//*[local-name()='DLieu']/*[local-name()='HDon']//*[local-name()='MCCQT'])[1])");
        String title = firstText(document,
                "string((//*[local-name()='DLTBao']/*[local-name()='Ten'])[1])",
                "string((//*[local-name()='TTChung']/*[local-name()='TDiep'])[1])");
        if (title.isBlank()) title = "Cơ quan thuế đã xử lý hóa đơn";

        invoice.setStatus((short) 3);
        if (!cqtCode.isBlank()) invoice.setCodeCqt(cqtCode);
        invoice.setUpdatedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        Node returnedInvoice = node(document, "(//*[local-name()='DLieu']/*[local-name()='HDon'])[1]");
        if (returnedInvoice != null) saveAuthorityTaxXml(invoice, serialize(returnedInvoice));

        upsertHistory(companyId, invoice.getUserId().longValue(), "invoices", invoice.getId(),
                messageType, title, cqtCode, xml);
    }

    private void processNotice(Document document, String xml, String reference,
                               long companyId, int messageType) {
        String noticeType = text(document, "string((" + NOTICE_PATH + "/*[local-name()='LTBao'])[1])");
        String title = text(document, "string((" + NOTICE_PATH + "/*[local-name()='Ten'])[1])");
        if (title.isBlank()) title = "Thông báo kết quả xử lý của cơ quan thuế";

        Optional<ErrorMessageEntity> errorMessage = errorMessageRepository
                .findFirstByMessageCodeAndCompanyIdOrderByIdDesc(reference, companyId);
        if (errorMessage.isPresent()) {
            processErrorNotice(document, xml, errorMessage.get(), messageType, noticeType, title);
            return;
        }

        InvoiceEntity invoice = findInvoice(reference, companyId);
        boolean accepted = "2".equals(noticeType);
        boolean hasCode = formInvoiceRepository.findById(invoice.getFormId().longValue())
                .map(FormInvoiceEntity::getHaveCode).orElse(0) == 1;
        String rejection = firstText(document,
                "string((" + NOTICE_PATH + "/*[local-name()='LCMa']/*[local-name()='DSLDo'])[1])",
                "string((" + NOTICE_PATH + "/*[local-name()='LHDKMa'])[1])",
                "string((" + NOTICE_PATH + "/*[local-name()='LHDMTTien']/*[local-name()='DSLDo'])[1])",
                "string((" + NOTICE_PATH + "/*[local-name()='LCTu']/*[local-name()='DSCTu']/*[local-name()='CTu'])[1])",
                "string((" + NOTICE_PATH + "/*[local-name()='KHLKhac'])[1])");
        String description = accepted
                ? text(document, "string((" + NOTICE_PATH + "/*[local-name()='CCu'])[1])")
                : rejection;

        // 204 + LCMa là từ chối cấp mã, kể cả khi LTBao không được gửi kèm.
        boolean hasCodeError = node(document, "(" + NOTICE_PATH + "/*[local-name()='LCMa'])[1]") != null;
        short status = (short) (accepted && !hasCodeError ? 3 : 7);
        invoice.setStatus(status);
        if (status == 7 || !hasCode) invoice.setCodeCqt(null);
        invoice.setUpdatedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        if (description.isBlank()) {
            description = status == 3 ? "Cơ quan thuế đã chấp nhận hóa đơn" : "Cơ quan thuế từ chối hóa đơn";
        }
        upsertHistory(companyId, invoice.getUserId().longValue(), "invoices", invoice.getId(),
                messageType, title, description, xml);
    }

    private void processErrorNotice(Document document, String xml, ErrorMessageEntity entity,
                                    int messageType, String noticeType, String title) {
        boolean accepted = "2".equals(noticeType);
        entity.setStatus(accepted ? 3 : 4);
        String responseNumber = text(document, "string((" + NOTICE_PATH + "/*[local-name()='So'])[1])");
        if (!responseNumber.isBlank()) entity.setTaxResponseNumber(responseNumber);
        entity.setUpdatedAt(LocalDateTime.now());
        errorMessageRepository.save(entity);

        String description = accepted
                ? text(document, "string((" + NOTICE_PATH + "/*[local-name()='CCu'])[1])")
                : firstText(document,
                    "string((" + NOTICE_PATH + "/*[local-name()='KHLKhac'])[1])",
                    "string((" + NOTICE_PATH + "/*[local-name()='LCTu']/*[local-name()='DSCTu']/*[local-name()='CTu'])[1])");
        upsertHistory(entity.getCompanyId(), entity.getUserId(), "error_messages", entity.getId(),
                messageType, title, description, xml);
    }

    private void processRegisterReceive(Document document, String xml, String reference,
                                        long companyId, int messageType) {
        RegisterInvoiceEntity register = findRegister(reference, companyId);
        String result = text(document, "string((//*[local-name()='DLTBao']/*[local-name()='THop'])[1])");
        boolean accepted = "1".equals(result) || "3".equals(result);
        register.setStatus(accepted ? 4 : 3);
        register.setResponseReceiveFile(text(document,
                "string((//*[local-name()='DLTBao']/*[local-name()='MSo'])[1])"));
        register.setUpdatedAt(LocalDateTime.now());
        registerInvoiceRepository.save(register);

        String title = text(document, "string((//*[local-name()='DLTBao']/*[local-name()='Ten'])[1])");
        upsertHistory(companyId, register.getUserId(), "register_invoices", register.getId(), messageType,
                title.isBlank() ? "Thông báo tiếp nhận tờ khai" : title,
                accepted ? "Cơ quan thuế đã tiếp nhận tờ khai" : "Cơ quan thuế chưa tiếp nhận tờ khai", xml);
    }

    private void processRegisterAccept(Document document, String xml, String reference,
                                       long companyId, int messageType) {
        RegisterInvoiceEntity register = findRegister(reference, companyId);
        boolean accepted = "1".equals(text(document,
                "string((//*[local-name()='DLTBao']/*[local-name()='TTXNCQT'])[1])"));
        register.setStatus(accepted ? 6 : 5);
        register.setResponseAcceptFile(firstText(document,
                "string((//*[local-name()='DLTBao']/*[local-name()='STBao']/*[local-name()='So'])[1])",
                "string((//*[local-name()='DLTBao']/*[local-name()='MSo'])[1])"));
        if (accepted && register.getEffectiveDate() == null) register.setEffectiveDate(LocalDateTime.now());
        register.setUpdatedAt(LocalDateTime.now());
        registerInvoiceRepository.save(register);

        String title = text(document, "string((//*[local-name()='DLTBao']/*[local-name()='Ten'])[1])");
        String description = accepted ? "Cơ quan thuế đã chấp nhận tờ khai" : firstText(document,
                "string((//*[local-name()='DLTBao']/*[local-name()='DSLDKCNhan'])[1])",
                "string((//*[local-name()='DLTBao']/*[local-name()='KHLKhac'])[1])");
        upsertHistory(companyId, register.getUserId(), "register_invoices", register.getId(), messageType,
                title.isBlank() ? "Thông báo kết quả tờ khai" : title, description, xml);
    }

    private void processErrorMessage301(Document document, String xml, String reference,
                                        long companyId, int messageType) {
        ErrorMessageEntity entity = errorMessageRepository
                .findFirstByMessageCodeAndCompanyIdOrderByIdDesc(reference, companyId)
                .orElseThrow(() -> new CallbackException("Không tìm thấy thông báo sai sót theo MTDTChieu " + reference));
        entity.setStatus(5);
        String responseNumber = text(document,
                "string((//*[local-name()='TBao']/*[local-name()='STBao']/*[local-name()='So'])[1])");
        if (!responseNumber.isBlank()) entity.setTaxResponseNumber(responseNumber);
        entity.setUpdatedAt(LocalDateTime.now());
        errorMessageRepository.save(entity);

        String title = text(document, "string((//*[local-name()='DLTBao']/*[local-name()='Ten'])[1])");
        upsertHistory(companyId, entity.getUserId(), "error_messages", entity.getId(), messageType,
                title.isBlank() ? "Cơ quan thuế đã nhận thông báo sai sót" : title,
                responseNumber, xml);
    }

    private void processGeneralError(Document document, String xml, String reference,
                                     long companyId, int messageType) {
        String description = firstText(document,
                "string((//*[local-name()='DLTBao']/*[local-name()='MTLoi'])[1])",
                "string((//*[local-name()='DLTBao']/*[local-name()='MTLoiChitiet'])[1])",
                "string((//*[local-name()='DLTBao'])[1])");
        Optional<InvoiceEntity> invoice = invoiceRepository
                .findFirstByCodeAndCompanyIdOrderByIdDesc(reference, Math.toIntExact(companyId));
        if (invoice.isPresent()) {
            InvoiceEntity entity = invoice.get();
            entity.setStatus((short) 7);
            entity.setUpdatedAt(LocalDateTime.now());
            invoiceRepository.save(entity);
            upsertHistory(companyId, entity.getUserId().longValue(), "invoices", entity.getId(), messageType,
                    "Cơ quan thuế trả về lỗi xử lý", description, xml);
            return;
        }
        throw new CallbackException("Không tìm thấy dữ liệu theo MTDTChieu " + reference);
    }

    private CompanyEntity resolveCompany(String reference, String taxpayerTaxCode) {
        Set<Long> matchedCompanyIds = new LinkedHashSet<>();
        invoiceRepository.findByCode(reference).stream()
                .map(InvoiceEntity::getCompanyId).filter(id -> id != null)
                .map(Integer::longValue).forEach(matchedCompanyIds::add);
        registerInvoiceRepository.findByCode(reference).stream()
                .map(RegisterInvoiceEntity::getCompanyId).filter(id -> id != null).forEach(matchedCompanyIds::add);
        registerInvoiceRepository.findByDeclarationCode(reference).stream()
                .map(RegisterInvoiceEntity::getCompanyId).filter(id -> id != null).forEach(matchedCompanyIds::add);
        errorMessageRepository.findByMessageCode(reference).stream()
                .map(ErrorMessageEntity::getCompanyId).filter(id -> id != null).forEach(matchedCompanyIds::add);

        Optional<CompanyEntity> byTaxCode = hasText(taxpayerTaxCode)
                ? companyRepository.findByTaxcode(taxpayerTaxCode.trim()) : Optional.empty();
        if (byTaxCode.isPresent() && (matchedCompanyIds.isEmpty()
                || matchedCompanyIds.contains(byTaxCode.get().getId()))) {
            return requireActiveCompany(byTaxCode.get());
        }
        if (matchedCompanyIds.size() == 1) {
            return requireActiveCompany(companyRepository.findById(matchedCompanyIds.iterator().next())
                    .orElseThrow(() -> new CallbackException("Không tìm thấy công ty nhận callback")));
        }
        if (matchedCompanyIds.isEmpty()) {
            throw new CallbackException("Không tìm thấy dữ liệu theo MTDTChieu " + reference);
        }
        throw new CallbackException("MTDTChieu trùng giữa nhiều công ty; cần mstNnt hợp lệ");
    }

    private CompanyEntity requireActiveCompany(CompanyEntity company) {
        if (!Integer.valueOf(1).equals(company.getStatus())) {
            throw new CallbackException("Công ty nhận callback không hoạt động");
        }
        return company;
    }

    private InvoiceEntity findInvoice(String reference, long companyId) {
        return invoiceRepository.findFirstByCodeAndCompanyIdOrderByIdDesc(reference, Math.toIntExact(companyId))
                .orElseThrow(() -> new CallbackException("Không tìm thấy hóa đơn theo MTDTChieu " + reference));
    }

    private RegisterInvoiceEntity findRegister(String reference, long companyId) {
        return registerInvoiceRepository.findFirstByCodeAndCompanyIdOrderByIdDesc(reference, companyId)
                .or(() -> registerInvoiceRepository.findFirstByDeclarationCodeAndCompanyIdOrderByIdDesc(reference, companyId))
                .orElseThrow(() -> new CallbackException("Không tìm thấy tờ khai theo MTDTChieu " + reference));
    }

    private static String registerCallbackXml(int messageType, String reference, boolean accepted, String reason) {
        String resultTag = messageType == 102 ? "THop" : "TTXNCQT";
        String result = accepted ? "1" : "0";
        String noticeNumber = "LOCAL-" + messageType + "-" + UUID.randomUUID().toString().replace("-", "");
        String detail = accepted ? "" : "<KHLKhac>" + escapeXml(reason) + "</KHLKhac>";
        String acceptNumber = messageType == 103
                ? "<STBao><So>" + escapeXml(noticeNumber) + "</So></STBao>"
                : "";
        return "<TDiep><TTChung><MLTDiep>" + messageType + "</MLTDiep><MTDTChieu>"
                + escapeXml(reference) + "</MTDTChieu></TTChung><DLieu><TBao><DLTBao><"
                + resultTag + ">" + result + "</" + resultTag + ">"
                + (messageType == 102 ? "<MSo>" + escapeXml(noticeNumber) + "</MSo>" : acceptNumber)
                + detail + "</DLTBao></TBao></DLieu></TDiep>";
    }

    private static String errorMessageCallbackXml(String reference) {
        String noticeNumber = "LOCAL-301-" + UUID.randomUUID().toString().replace("-", "");
        return "<TDiep><TTChung><MLTDiep>301</MLTDiep><MTDTChieu>"
                + escapeXml(reference) + "</MTDTChieu></TTChung><DLieu><TBao><STBao><So>"
                + escapeXml(noticeNumber) + "</So></STBao></TBao></DLieu></TDiep>";
    }

    private void saveAuthorityTaxXml(InvoiceEntity invoice, String returnedInvoiceXml) {
        SignatureAuthoritiesTaxEntity signature = signatureAuthoritiesTaxRepository
                .findTopByInvoiceIdAndCompanyIdOrderByIdDesc(invoice.getId().intValue(), invoice.getCompanyId())
                .orElseGet(SignatureAuthoritiesTaxEntity::new);
        LocalDateTime now = LocalDateTime.now();
        signature.setCompanyId(invoice.getCompanyId());
        signature.setInvoiceId(invoice.getId().intValue());
        signature.setXml(returnedInvoiceXml);
        if (signature.getCreatedAt() == null) signature.setCreatedAt(now);
        signature.setUpdatedAt(now);
        signatureAuthoritiesTaxRepository.save(signature);
    }

    private void upsertHistory(long companyId, long userId, String tableName, long tableId,
                               int type, String title, String description, String xml) {
        HistoryEntity history = historyRepository
                .findFirstByCompanyIdAndTableNameAndTableIdAndTypeOrderByIdDesc(
                        companyId, tableName, tableId, type)
                .orElseGet(HistoryEntity::new);
        history.setCompanyId(companyId);
        history.setUserId(userId);
        history.setTableName(tableName);
        history.setTableId(tableId);
        history.setType(type);
        history.setTitle(title);
        history.setDescription(description);
        history.setShowNotify(1);
        history.setStatus(1);
        history.setXmlData(xml);
        historyRepository.save(history);
    }

    private static String acceptedCallback(String reference, boolean haveCode, String invoiceXml) {
        if (haveCode) {
            String cqtCode = "C0" + UUID.randomUUID().toString().replace("-", "");
            String hdon = XML_DECLARATION.matcher(invoiceXml == null ? "" : invoiceXml).replaceFirst("").trim();
            Matcher close = HDON_CLOSE.matcher(hdon);
            if (!close.find()) {
                return rejectedCallback(reference, true, "Không tìm thấy thẻ đóng HDon trong XML đã ký");
            }
            hdon = hdon.substring(0, close.start()) + "<MCCQT>" + cqtCode + "</MCCQT>" + close.group();
            return "<TDiep><TTChung><MLTDiep>202</MLTDiep><MTDTChieu>"
                    + escapeXml(reference) + "</MTDTChieu></TTChung><DLieu>"
                    + hdon + "</DLieu></TDiep>";
        }
        return "<TDiep><TTChung><MLTDiep>204</MLTDiep><MTDTChieu>"
                + escapeXml(reference)
                + "</MTDTChieu></TTChung><DLieu><TBao><DLTBao>"
                + "<LTBao>2</LTBao><Ten>Thông báo hóa đơn điện tử không có mã</Ten>"
                + "<CCu>Hóa đơn hợp lệ, cơ quan thuế đã tiếp nhận</CCu>"
                + "</DLTBao></TBao></DLieu></TDiep>";
    }

    private static String rejectedCallback(String reference, boolean haveCode, String reason) {
        if (haveCode) {
            return "<TDiep><TTChung><MLTDiep>204</MLTDiep><MTDTChieu>"
                    + escapeXml(reference)
                    + "</MTDTChieu></TTChung><DLieu><TBao><DLTBao>"
                    + "<Ten>Thông báo hóa đơn không đủ điều kiện cấp mã</Ten>"
                    + "<LCMa><DSLDo>" + escapeXml(reason) + "</DSLDo></LCMa>"
                    + "</DLTBao></TBao></DLieu></TDiep>";
        }
        return "<TDiep><TTChung><MLTDiep>204</MLTDiep><MTDTChieu>"
                + escapeXml(reference)
                + "</MTDTChieu></TTChung><DLieu><TBao><DLTBao>"
                + "<LTBao>3</LTBao><Ten>Thông báo hóa đơn điện tử không hợp lệ</Ten>"
                + "<LHDKMa>" + escapeXml(reason) + "</LHDKMa>"
                + "</DLTBao></TBao></DLieu></TDiep>";
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            setFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            setFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
            setFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
            setFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new CallbackException("XML callback không đúng định dạng: " + e.getMessage());
        }
    }

    private static void setFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignored) {
            // Trình phân tích XML có thể không hỗ trợ toàn bộ các cờ bảo mật.
        }
    }

    private static String text(Document document, String expression) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            String value = (String) xpath.evaluate(expression, document, XPathConstants.STRING);
            return value == null ? "" : value.trim();
        } catch (Exception e) {
            throw new CallbackException("Không đọc được XML callback: " + e.getMessage());
        }
    }

    private static Node node(Document document, String expression) {
        try {
            return (Node) XPathFactory.newInstance().newXPath()
                    .evaluate(expression, document, XPathConstants.NODE);
        } catch (Exception e) {
            throw new CallbackException("Không đọc được XML callback: " + e.getMessage());
        }
    }

    private static String firstText(Document document, String... expressions) {
        for (String expression : expressions) {
            String value = text(document, expression);
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private static String serialize(Node node) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            try {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            } catch (Exception ignored) {
                // Không phải TransformerFactory nào cũng hỗ trợ tất cả thuộc tính.
            }
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new CallbackException("Không lưu được XML CQT: " + e.getMessage());
        }
    }

    private static int parseMessageType(String value) {
        requireText(value, "Thông điệp callback thiếu MLTDiep");
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new CallbackException("MLTDiep không hợp lệ: " + value);
        }
    }

    private static void requireText(String value, String message) {
        if (!hasText(value)) throw new CallbackException(message);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public record Result(int status, String message, int messageType, String messageReference, long companyId) {}

    public static class CallbackException extends RuntimeException {
        public CallbackException(String message) {
            super(message);
        }
    }
}
