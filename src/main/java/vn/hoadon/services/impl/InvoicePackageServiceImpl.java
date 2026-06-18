package vn.hoadon.services.impl;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vn.hoadon.dto.invoicepackage.InvoicePackageFilterDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageMonthlyStatisticDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackagePurchaseDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackagePurchaseFilterDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageRequestDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageResponseDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageStatisticsDTO;
import vn.hoadon.config.ZaloPayProperties;
import vn.hoadon.entity.BuyInvoiceEntity;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.InvoicePackageEntity;
import vn.hoadon.entity.InvoicePackagePurchaseEntity;
import vn.hoadon.entity.MailTemplateEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.messaging.MailJobMessage;
import vn.hoadon.repositories.BuyInvoiceRepository;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.InvoicePackagePurchaseRepository;
import vn.hoadon.repositories.InvoicePackageRepository;
import vn.hoadon.repositories.MailTemplateRepository;
import vn.hoadon.services.BuyInvoiceHistoryService;
import vn.hoadon.services.InvoicePackageService;
import vn.hoadon.services.MailQueueService;
import vn.hoadon.services.MomoPaymentService;
import vn.hoadon.services.VnpayPaymentService;
import vn.hoadon.services.ZaloPayPaymentService;
import vn.hoadon.util.SystemMail;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.UUID;

@Service
public class InvoicePackageServiceImpl implements InvoicePackageService {

    private static final Logger log = LoggerFactory.getLogger(InvoicePackageServiceImpl.class);
    private static final String MAIL_KEY = "BUY_INVOICE_MAIL";
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    @Autowired private InvoicePackageRepository packageRepository;
    @Autowired private InvoicePackagePurchaseRepository purchaseRepository;
    @Autowired private BuyInvoiceRepository buyInvoiceRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private MailTemplateRepository mailTemplateRepository;
    @Autowired private MailQueueService mailQueueService;
    @Autowired private BuyInvoiceHistoryService buyInvoiceHistoryService;
    @Autowired private MomoPaymentService momoPaymentService;
    @Autowired private VnpayPaymentService vnpayPaymentService;
    @Autowired private ZaloPayPaymentService zaloPayPaymentService;
    @Autowired private ZaloPayProperties zaloPayProperties;
    @Autowired private ObjectMapper objectMapper;

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    @Value("${app.backend-url:http://localhost:8081}")
    private String backendUrl;

    @Override
    public Page<InvoicePackageResponseDTO> listPackages(InvoicePackageFilterDTO filter, Pageable pageable) {
        ensureDefaultPackage();
        Specification<InvoicePackageEntity> spec = packageSpec(filter);
        return packageRepository.findAll(spec, pageable).map(this::toPackageDto);
    }

    @Override
    public List<InvoicePackageResponseDTO> listActivePackages() {
        ensureDefaultPackage();
        return packageRepository.findByStatusOrderByDisplayOrderAscIdAsc(1)
                .stream()
                .map(this::toPackageDto)
                .toList();
    }

    @Override
    @Transactional
    public InvoicePackageResponseDTO savePackage(InvoicePackageRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu gói hóa đơn");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên gói hóa đơn không được để trống");
        }
        if (dto.getInvoiceQuantity() == null || dto.getInvoiceQuantity() <= 0) {
            throw new IllegalArgumentException("Số hóa đơn phải lớn hơn 0");
        }
        if (dto.getUnitPrice() == null || dto.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Đơn giá không hợp lệ");
        }

        InvoicePackageEntity entity = dto.getId() != null
                ? packageRepository.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói hóa đơn"))
                : new InvoicePackageEntity();

        entity.setName(dto.getName().trim());
        entity.setInvoiceQuantity(dto.getInvoiceQuantity());
        entity.setUnitPrice(money(dto.getUnitPrice()));
        entity.setTotalPrice(resolveTotalPrice(dto));
        entity.setIncludeTrial(Boolean.TRUE.equals(dto.getIncludeTrial()));
        entity.setDescription(trimToNull(dto.getDescription()));
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);

        return toPackageDto(packageRepository.save(entity));
    }

    @Override
    public void deletePackage(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Thiếu ID gói hóa đơn");
        }
        InvoicePackageEntity entity = packageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói hóa đơn"));
        entity.setStatus(0);
        packageRepository.save(entity);
    }

    @Override
    @Transactional
    public InvoicePackagePurchaseDTO purchase(Long packageId, String paymentMethod, UserEntity user) {
        PurchaseContext context = preparePurchase(packageId, paymentMethod, user);
        InvoicePackagePurchaseEntity purchase = newPurchase(context.invoicePackage(), context.company(), context.user(), context.method());

        if (isMomoPaymentMethod(context.method())) {
            return createMomoPurchase(purchase, context.company());
        }
        if ("VNPAY".equals(context.method())) {
            return createVnpayPurchase(purchase, context.company());
        }
        if ("ZALOPAY".equals(context.method())) {
            return createZaloPayPurchase(purchase, context.company());
        }

        purchase.setPaymentStatus("SUCCESS");
        purchase.setPaymentCode(fakePaymentCode(context.method()));
        purchase.setPaidAt(LocalDateTime.now());
        purchase.setNote("Thanh toán giả lập thành công");
        purchase = purchaseRepository.save(purchase);
        return completeSuccessfulPurchase(
                purchase,
                context.company(),
                context.user(),
                "CUSTOMER",
                "Khách hàng mua gói hóa đơn",
                "Thanh toán giả lập thành công"
        );
    }

    @Override
    @Transactional
    public InvoicePackagePurchaseDTO retryPayment(Long purchaseId, UserEntity user) {
        if (purchaseId == null) {
            throw new IllegalArgumentException("Thiếu mã giao dịch mua gói");
        }
        if (user == null || user.getCompanyId() == null) {
            throw new IllegalArgumentException("Không xác định được công ty");
        }

        InvoicePackagePurchaseEntity purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch mua gói"));
        if (!user.getCompanyId().equals(purchase.getCompanyId())) {
            throw new IllegalArgumentException("Giao dịch không thuộc công ty hiện tại");
        }

        String status = purchase.getPaymentStatus() != null
                ? purchase.getPaymentStatus().trim().toUpperCase(Locale.ROOT)
                : "";
        if ("SUCCESS".equals(status) || purchase.getBuyInvoiceId() != null) {
            throw new IllegalArgumentException("Giao dịch đã thanh toán thành công");
        }
        if (!"PENDING".equals(status) && !"FAILED".equals(status)) {
            throw new IllegalArgumentException("Chỉ có thể thanh toán lại giao dịch đang chờ hoặc thất bại");
        }

        String method = purchase.getPaymentMethod() != null
                ? purchase.getPaymentMethod().trim().toUpperCase(Locale.ROOT)
                : "";
        method = normalizePaymentMethod(method);
        if (!isMomoPaymentMethod(method) && !"VNPAY".equals(method) && !"ZALOPAY".equals(method)) {
            throw new IllegalArgumentException("Phương thức thanh toán không hỗ trợ thanh toán lại");
        }
        CompanyEntity company = companyRepository.findById(purchase.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công ty"));

        InvoicePackagePurchaseEntity retryPurchase = retryPurchaseFrom(purchase, method);
        if (isMomoPaymentMethod(method)) {
            return createMomoCheckout(
                    retryPurchase,
                    company,
                    "Đang chờ thanh toán lại " + momoPaymentLabel(method) + " từ giao dịch #" + purchase.getId(),
                    "Đã tạo lại giao dịch " + momoPaymentLabel(method) + ", chờ khách hàng thanh toán"
            );
        }
        if ("ZALOPAY".equals(method)) {
            return createZaloPayCheckout(
                    retryPurchase,
                    company,
                    "Đang chờ thanh toán lại ZaloPay từ giao dịch #" + purchase.getId(),
                    "Đã tạo lại giao dịch ZaloPay, chờ khách hàng thanh toán"
            );
        }
        return createVnpayCheckout(
                retryPurchase,
                company,
                "Đang chờ thanh toán lại VNPAY từ giao dịch #" + purchase.getId(),
                "Đã tạo lại giao dịch VNPAY, chờ khách hàng thanh toán"
        );
    }

    @Override
    @Transactional
    public InvoicePackagePurchaseDTO handleMomoIpn(Map<String, Object> payload) {
        InvoicePackagePurchaseDTO purchase = handleMomoCallback(payload, "MOMO_IPN");
        purchase.setPaymentMessage("Đã nhận kết quả thanh toán MoMo");
        return purchase;
    }

    @Override
    @Transactional
    public String handleMomoReturn(Map<String, String> params) {
        try {
            InvoicePackagePurchaseDTO purchase = handleMomoCallback(params, "MOMO_RETURN");
            String status = "SUCCESS".equals(purchase.getPaymentStatus()) ? "success" : "failed";
            return buildFrontendMomoRedirect(status, purchase.getPaymentCode(), purchase.getPaymentMessage());
        } catch (Exception e) {
            log.warn("Không thể xử lý redirect MoMo: {}", e.getMessage());
            String orderId = params != null ? params.get("orderId") : null;
            return buildFrontendMomoRedirect("failed", orderId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, String> handleVnpayIpn(Map<String, String> params) {
        try {
            return handleVnpayIpnInternal(params);
        } catch (Exception e) {
            log.warn("Không thể xử lý IPN VNPAY: {}", e.getMessage());
            return vnpayResponse("99", "Unknown error");
        }
    }

    @Override
    @Transactional
    public String handleVnpayReturn(Map<String, String> params) {
        try {
            InvoicePackagePurchaseDTO purchase = handleVnpayCallback(params, "VNPAY_RETURN");
            String status = "SUCCESS".equals(purchase.getPaymentStatus()) ? "success" : "failed";
            return buildFrontendPaymentRedirect("vnpayStatus", status, purchase.getPaymentCode(), purchase.getPaymentMessage());
        } catch (Exception e) {
            log.warn("Không thể xử lý redirect VNPAY: {}", e.getMessage());
            String orderId = params != null ? params.get("vnp_TxnRef") : null;
            return buildFrontendPaymentRedirect("vnpayStatus", "failed", orderId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> handleZaloPayCallback(Map<String, Object> payload) {
        try {
            handleZaloPayCallbackInternal(payload, "ZALOPAY_CALLBACK");
            return zaloPayResponse(1, "success");
        } catch (IllegalArgumentException e) {
            log.warn("Callback ZaloPay không hợp lệ: {}", e.getMessage());
            return zaloPayResponse(-1, e.getMessage());
        } catch (Exception e) {
            log.warn("Không thể xử lý callback ZaloPay: {}", e.getMessage());
            return zaloPayResponse(0, e.getMessage());
        }
    }

    @Override
    @Transactional
    public String handleZaloPayReturn(Map<String, String> params) {
        try {
            InvoicePackagePurchaseDTO purchase = handleZaloPayRedirect(params, "ZALOPAY_RETURN");
            String status = "SUCCESS".equals(purchase.getPaymentStatus())
                    ? "success"
                    : "PENDING".equals(purchase.getPaymentStatus()) ? "pending" : "failed";
            return buildFrontendPaymentRedirect("zalopayStatus", status, purchase.getPaymentCode(), purchase.getPaymentMessage());
        } catch (Exception e) {
            log.warn("Không thể xử lý redirect ZaloPay: {}", e.getMessage());
            String orderId = params != null ? firstNotBlank(params.get("app_trans_id"), params.get("apptransid")) : null;
            return buildFrontendPaymentRedirect("zalopayStatus", "failed", orderId, e.getMessage());
        }
    }

    @Override
    public InvoicePackagePurchaseDTO getMyPurchase(Long purchaseId, UserEntity user) {
        if (purchaseId == null) {
            throw new IllegalArgumentException("Thiếu mã giao dịch mua gói");
        }
        if (user == null || user.getCompanyId() == null) {
            throw new IllegalArgumentException("Không xác định được công ty");
        }
        InvoicePackagePurchaseEntity purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch mua gói"));
        if (!user.getCompanyId().equals(purchase.getCompanyId())) {
            throw new IllegalArgumentException("Giao dịch không thuộc công ty hiện tại");
        }
        CompanyEntity company = companyRepository.findById(purchase.getCompanyId()).orElse(null);
        return dtoWithInvoiceState(purchase, company);
    }

    private PurchaseContext preparePurchase(Long packageId, String paymentMethod, UserEntity user) {
        if (user == null || user.getCompanyId() == null) {
            throw new IllegalArgumentException("Không xác định được công ty mua gói");
        }
        if (packageId == null) {
            throw new IllegalArgumentException("Vui lòng chọn gói hóa đơn");
        }

        InvoicePackageEntity invoicePackage = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói hóa đơn"));
        if (!Integer.valueOf(1).equals(invoicePackage.getStatus())) {
            throw new IllegalArgumentException("Gói hóa đơn chưa được kích hoạt");
        }

        CompanyEntity company = companyRepository.findById(user.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công ty"));
        return new PurchaseContext(invoicePackage, company, user, normalizePaymentMethod(paymentMethod));
    }

    private InvoicePackagePurchaseEntity newPurchase(InvoicePackageEntity invoicePackage, CompanyEntity company,
                                                     UserEntity user, String method) {
        InvoicePackagePurchaseEntity purchase = new InvoicePackagePurchaseEntity();
        purchase.setPackageId(invoicePackage.getId());
        purchase.setPackageName(invoicePackage.getName());
        purchase.setCompanyId(company.getId());
        purchase.setCompanyName(company.getName());
        purchase.setCompanyTaxcode(company.getTaxcode());
        purchase.setBuyerName(resolveBuyerName(user, company));
        purchase.setBuyerEmail(resolveBuyerEmail(user, company));
        purchase.setBuyerPhone(resolveBuyerPhone(user, company));
        purchase.setInvoiceQuantity(invoicePackage.getInvoiceQuantity());
        purchase.setUnitPrice(money(invoicePackage.getUnitPrice()));
        purchase.setTotalPrice(money(invoicePackage.getTotalPrice()));
        purchase.setPaymentMethod(method);
        return purchase;
    }

    private InvoicePackagePurchaseEntity retryPurchaseFrom(InvoicePackagePurchaseEntity source, String method) {
        InvoicePackagePurchaseEntity purchase = new InvoicePackagePurchaseEntity();
        purchase.setPackageId(source.getPackageId());
        purchase.setPackageName(source.getPackageName());
        purchase.setCompanyId(source.getCompanyId());
        purchase.setCompanyName(source.getCompanyName());
        purchase.setCompanyTaxcode(source.getCompanyTaxcode());
        purchase.setBuyerName(source.getBuyerName());
        purchase.setBuyerEmail(source.getBuyerEmail());
        purchase.setBuyerPhone(source.getBuyerPhone());
        purchase.setInvoiceQuantity(source.getInvoiceQuantity());
        purchase.setUnitPrice(money(source.getUnitPrice()));
        purchase.setTotalPrice(money(source.getTotalPrice()));
        purchase.setPaymentMethod(method);
        return purchase;
    }

    private InvoicePackagePurchaseDTO createMomoPurchase(InvoicePackagePurchaseEntity purchase, CompanyEntity company) {
        return createMomoCheckout(
                purchase,
                company,
                "Đang chờ thanh toán " + momoPaymentLabel(purchase.getPaymentMethod()),
                "Đã tạo giao dịch " + momoPaymentLabel(purchase.getPaymentMethod()) + ", chờ khách hàng thanh toán"
        );
    }

    private InvoicePackagePurchaseDTO createMomoCheckout(InvoicePackagePurchaseEntity purchase, CompanyEntity company,
                                                         String waitingNote, String paymentMessage) {
        purchase.setPaymentStatus("PENDING");
        purchase.setPaidAt(null);
        purchase.setNote(waitingNote);
        purchase = purchaseRepository.save(purchase);

        String orderId = buildMomoOrderId(purchase.getId());
        String requestId = buildMomoRequestId(purchase.getId());
        purchase.setPaymentCode(orderId);
        purchase = purchaseRepository.save(purchase);

        MomoPaymentService.CreatePaymentResponse momoResponse;
        try {
            momoResponse = momoPaymentService.createPayment(
                    new MomoPaymentService.CreatePaymentRequest(
                            orderId,
                            requestId,
                            amountLong(purchase.getTotalPrice()),
                            "Thanh toán gói hóa đơn " + nullToBlank(purchase.getPackageName()),
                            buildMomoExtraData(purchase),
                            null,
                            null,
                            momoRequestType(purchase.getPaymentMethod()),
                            momoUserInfo(purchase, purchase.getPaymentMethod())
                    )
            );
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        purchase.setNote("Đã tạo giao dịch " + momoPaymentLabel(purchase.getPaymentMethod()) + ": " + nullToBlank(momoResponse.message()));
        purchase = purchaseRepository.save(purchase);

        InvoicePackagePurchaseDTO dto = dtoWithInvoiceState(purchase, company);
        dto.setPayUrl(momoResponse.payUrl());
        dto.setDeeplink(momoResponse.deeplink());
        dto.setQrCodeUrl(momoResponse.qrCodeUrl());
        dto.setPaymentMessage(paymentMessage);
        return dto;
    }

    private InvoicePackagePurchaseDTO createVnpayPurchase(InvoicePackagePurchaseEntity purchase, CompanyEntity company) {
        return createVnpayCheckout(
                purchase,
                company,
                "Đang chờ thanh toán VNPAY",
                "Đã tạo giao dịch VNPAY, chờ khách hàng thanh toán"
        );
    }

    private InvoicePackagePurchaseDTO createVnpayCheckout(InvoicePackagePurchaseEntity purchase, CompanyEntity company,
                                                          String waitingNote, String paymentMessage) {
        purchase.setPaymentStatus("PENDING");
        purchase.setPaidAt(null);
        purchase.setNote(waitingNote);
        purchase = purchaseRepository.save(purchase);

        String txnRef = buildVnpayTxnRef(purchase.getId());
        purchase.setPaymentCode(txnRef);
        purchase = purchaseRepository.save(purchase);

        VnpayPaymentService.CreatePaymentResponse vnpayResponse;
        try {
            vnpayResponse = vnpayPaymentService.createPaymentUrl(
                    new VnpayPaymentService.CreatePaymentRequest(
                            txnRef,
                            amountLong(purchase.getTotalPrice()),
                            buildAsciiOrderInfo(purchase),
                            currentRequestIp(),
                            null
                    )
            );
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        purchase.setNote(vnpayResponse.message());
        purchase = purchaseRepository.save(purchase);

        InvoicePackagePurchaseDTO dto = dtoWithInvoiceState(purchase, company);
        dto.setPayUrl(vnpayResponse.payUrl());
        dto.setPaymentMessage(paymentMessage);
        return dto;
    }

    private InvoicePackagePurchaseDTO createZaloPayPurchase(InvoicePackagePurchaseEntity purchase, CompanyEntity company) {
        return createZaloPayCheckout(
                purchase,
                company,
                "Đang chờ thanh toán ZaloPay",
                "Đã tạo giao dịch ZaloPay, chờ khách hàng thanh toán"
        );
    }

    private InvoicePackagePurchaseDTO createZaloPayCheckout(InvoicePackagePurchaseEntity purchase, CompanyEntity company,
                                                            String waitingNote, String paymentMessage) {
        purchase.setPaymentStatus("PENDING");
        purchase.setPaidAt(null);
        purchase.setNote(waitingNote);
        purchase = purchaseRepository.save(purchase);

        String appTransId = buildZaloPayAppTransId(purchase.getId());
        purchase.setPaymentCode(appTransId);
        purchase = purchaseRepository.save(purchase);

        String redirectUrl = firstNotBlank(
                zaloPayProperties.getRedirectUrl(),
                trimTrailingSlash(backendUrl) + "/v1/invoice-packages/zalopay/return"
        );
        String callbackUrl = firstNotBlank(
                zaloPayProperties.getCallbackUrl(),
                trimTrailingSlash(backendUrl) + "/v1/invoice-packages/zalopay/callback"
        );

        ZaloPayPaymentService.CreatePaymentResponse zaloPayResponse;
        try {
            zaloPayResponse = zaloPayPaymentService.createPayment(
                    new ZaloPayPaymentService.CreatePaymentRequest(
                            appTransId,
                            amountLong(purchase.getTotalPrice()),
                            buildZaloPayAppUser(purchase),
                            "Hóa đơn - Thanh toán gói " + nullToBlank(purchase.getPackageName()),
                            buildZaloPayEmbedData(purchase, redirectUrl),
                            buildZaloPayItems(purchase),
                            null,
                            redirectUrl,
                            callbackUrl
                    )
            );
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        purchase.setNote(zaloPayResponse.message());
        purchase = purchaseRepository.save(purchase);

        InvoicePackagePurchaseDTO dto = dtoWithInvoiceState(purchase, company);
        dto.setPayUrl(zaloPayResponse.payUrl());
        dto.setPaymentMessage(paymentMessage);
        return dto;
    }

    private Map<String, String> handleVnpayIpnInternal(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return vnpayResponse("99", "Input data required");
        }
        if (!vnpayPaymentService.verifyCallbackSignature(params)) {
            return vnpayResponse("97", "Invalid signature");
        }

        String txnRef = vnpayPaymentService.value(params, "vnp_TxnRef");
        Optional<InvoicePackagePurchaseEntity> purchaseOpt = purchaseRepository.findByPaymentCode(txnRef);
        if (purchaseOpt.isEmpty()) {
            return vnpayResponse("01", "Order not found");
        }

        InvoicePackagePurchaseEntity purchase = purchaseOpt.get();
        CompanyEntity company = companyRepository.findById(purchase.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công ty của giao dịch VNPAY"));
        if (!isValidVnpayCallback(purchase, params)) {
            return vnpayResponse("04", "Invalid amount");
        }

        if ("SUCCESS".equals(purchase.getPaymentStatus()) && purchase.getBuyInvoiceId() != null) {
            return vnpayResponse("02", "Order already confirmed");
        }

        handleVnpayCallbackResult(purchase, company, params, "VNPAY_IPN");
        return vnpayResponse("00", "Confirm success");
    }

    private InvoicePackagePurchaseDTO handleVnpayCallback(Map<String, ?> params, String source) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("VNPAY không gửi dữ liệu thanh toán");
        }
        if (!vnpayPaymentService.verifyCallbackSignature(params)) {
            throw new IllegalArgumentException("Chữ ký VNPAY không hợp lệ");
        }

        String txnRef = vnpayPaymentService.value(params, "vnp_TxnRef");
        if (txnRef.isBlank()) {
            throw new IllegalArgumentException("VNPAY không gửi vnp_TxnRef");
        }

        InvoicePackagePurchaseEntity purchase = purchaseRepository.findByPaymentCode(txnRef)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch VNPAY: " + txnRef));
        CompanyEntity company = companyRepository.findById(purchase.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công ty của giao dịch VNPAY"));
        if (!isValidVnpayCallback(purchase, params)) {
            throw new IllegalArgumentException("Số tiền hoặc mã website VNPAY không khớp giao dịch");
        }

        return handleVnpayCallbackResult(purchase, company, params, source);
    }

    private InvoicePackagePurchaseDTO handleVnpayCallbackResult(InvoicePackagePurchaseEntity purchase,
                                                                CompanyEntity company,
                                                                Map<String, ?> params,
                                                                String source) {
        if (vnpayPaymentService.isSuccessResult(params)) {
            String transactionNo = vnpayPaymentService.value(params, "vnp_TransactionNo");
            String note = "VNPAY xác nhận thanh toán thành công"
                    + (!transactionNo.isBlank() ? ", transactionNo=" + transactionNo : "");
            InvoicePackagePurchaseDTO dto = completeSuccessfulPurchase(purchase, company, null, source, note, note);
            dto.setPaymentMessage(note);
            return dto;
        }

        String message = "VNPAY thanh toán không thành công: responseCode="
                + vnpayPaymentService.value(params, "vnp_ResponseCode")
                + ", transactionStatus=" + vnpayPaymentService.value(params, "vnp_TransactionStatus");
        InvoicePackagePurchaseDTO dto = failVnpayPurchase(purchase, company, message);
        dto.setPaymentMessage(message);
        return dto;
    }

    private boolean isValidVnpayCallback(InvoicePackagePurchaseEntity purchase, Map<String, ?> params) {
        String tmnCode = vnpayPaymentService.value(params, "vnp_TmnCode");
        long expectedAmount = amountLong(purchase.getTotalPrice()) * 100;
        long actualAmount = parseLong(vnpayPaymentService.value(params, "vnp_Amount"), -1);
        return vnpayPaymentService.isExpectedTmnCode(tmnCode) && actualAmount == expectedAmount;
    }

    private InvoicePackagePurchaseDTO failVnpayPurchase(InvoicePackagePurchaseEntity purchase, CompanyEntity company,
                                                        String message) {
        if (!"SUCCESS".equals(purchase.getPaymentStatus())) {
            purchase.setPaymentStatus("FAILED");
            purchase.setNote(message);
            purchase = purchaseRepository.save(purchase);
        }
        return dtoWithInvoiceState(purchase, company);
    }

    private InvoicePackagePurchaseDTO handleZaloPayCallbackInternal(Map<String, ?> payload, String source) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("ZaloPay không gửi dữ liệu thanh toán");
        }

        ZaloPayPaymentService.CallbackPayment callback = zaloPayPaymentService.verifyCallback(payload);
        InvoicePackagePurchaseEntity purchase = purchaseRepository.findByPaymentCode(callback.appTransId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch ZaloPay: " + callback.appTransId()));
        CompanyEntity company = companyRepository.findById(purchase.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công ty của giao dịch ZaloPay"));
        validateZaloPayPayment(purchase, callback.appId(), callback.amount());

        String note = "ZaloPay xác nhận thanh toán thành công"
                + (!isBlank(callback.zpTransId()) ? ", zpTransId=" + callback.zpTransId() : "");
        InvoicePackagePurchaseDTO dto = completeSuccessfulPurchase(purchase, company, null, source, note, note);
        dto.setPaymentMessage(note);
        return dto;
    }

    private InvoicePackagePurchaseDTO handleZaloPayRedirect(Map<String, ?> params, String source) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("ZaloPay không gửi dữ liệu redirect");
        }
        if (!zaloPayPaymentService.verifyRedirectChecksum(params)) {
            throw new IllegalArgumentException("Chữ ký redirect ZaloPay không hợp lệ");
        }

        String appTransId = firstNotBlank(
                zaloPayPaymentService.value(params, "app_trans_id"),
                zaloPayPaymentService.value(params, "apptransid")
        );
        if (appTransId == null || appTransId.isBlank()) {
            throw new IllegalArgumentException("ZaloPay không gửi app_trans_id");
        }

        InvoicePackagePurchaseEntity purchase = purchaseRepository.findByPaymentCode(appTransId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch ZaloPay: " + appTransId));
        CompanyEntity company = companyRepository.findById(purchase.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công ty của giao dịch ZaloPay"));
        validateZaloPayPayment(
                purchase,
                firstNotBlank(zaloPayPaymentService.value(params, "app_id"), zaloPayPaymentService.value(params, "appid")),
                parseLong(zaloPayPaymentService.value(params, "amount"), -1)
        );

        if ("SUCCESS".equals(purchase.getPaymentStatus()) && purchase.getBuyInvoiceId() != null) {
            InvoicePackagePurchaseDTO dto = dtoWithInvoiceState(purchase, company);
            dto.setPaymentMessage("ZaloPay đã xác nhận thanh toán trước đó");
            return dto;
        }

        if (!zaloPayPaymentService.isSuccessRedirect(params)) {
            String message = "ZaloPay thanh toán không thành công: status=" + zaloPayPaymentService.value(params, "status");
            InvoicePackagePurchaseDTO dto = failZaloPayPurchase(purchase, company, message);
            dto.setPaymentMessage(message);
            return dto;
        }

        try {
            ZaloPayPaymentService.QueryStatusResponse query = zaloPayPaymentService.queryStatus(appTransId);
            if (query.returnCode() == 1) {
                validateZaloPayPayment(purchase, null, query.amount());
                String note = "ZaloPay xác nhận thanh toán thành công"
                        + (!isBlank(query.zpTransId()) ? ", zpTransId=" + query.zpTransId() : "");
                InvoicePackagePurchaseDTO dto = completeSuccessfulPurchase(purchase, company, null, source, note, note);
                dto.setPaymentMessage(note);
                return dto;
            }
            if (query.returnCode() == 3 || query.processing()) {
                purchase.setPaymentStatus("PENDING");
                purchase.setNote("ZaloPay đang xử lý giao dịch: " + nullToBlank(query.returnMessage()));
                purchase = purchaseRepository.save(purchase);
                InvoicePackagePurchaseDTO dto = dtoWithInvoiceState(purchase, company);
                dto.setPaymentMessage("ZaloPay đang xử lý giao dịch, hệ thống sẽ cập nhật khi có callback");
                return dto;
            }

            String message = "ZaloPay thanh toán không thành công: returnCode="
                    + query.returnCode()
                    + ", message=" + nullToBlank(query.returnMessage());
            InvoicePackagePurchaseDTO dto = failZaloPayPurchase(purchase, company, message);
            dto.setPaymentMessage(message);
            return dto;
        } catch (Exception e) {
            log.warn("Không thể truy vấn trạng thái ZaloPay cho {}: {}", appTransId, e.getMessage());
            purchase.setPaymentStatus("PENDING");
            purchase.setNote("Đã nhận redirect ZaloPay hợp lệ, đang chờ callback xác nhận");
            purchase = purchaseRepository.save(purchase);
            InvoicePackagePurchaseDTO dto = dtoWithInvoiceState(purchase, company);
            dto.setPaymentMessage("Đã nhận redirect ZaloPay, hệ thống đang chờ callback xác nhận");
            return dto;
        }
    }

    private void validateZaloPayPayment(InvoicePackagePurchaseEntity purchase, String appId, long amount) {
        if (!isBlank(appId) && !zaloPayPaymentService.isExpectedAppId(appId)) {
            throw new IllegalArgumentException("app_id ZaloPay không khớp cấu hình");
        }

        long expectedAmount = amountLong(purchase.getTotalPrice());
        if (amount >= 0 && amount != expectedAmount) {
            throw new IllegalArgumentException("Số tiền ZaloPay không khớp giao dịch");
        }
    }

    private InvoicePackagePurchaseDTO failZaloPayPurchase(InvoicePackagePurchaseEntity purchase, CompanyEntity company,
                                                          String message) {
        if (!"SUCCESS".equals(purchase.getPaymentStatus())) {
            purchase.setPaymentStatus("FAILED");
            purchase.setNote(message);
            purchase = purchaseRepository.save(purchase);
        }
        return dtoWithInvoiceState(purchase, company);
    }

    private Map<String, Object> zaloPayResponse(int returnCode, String message) {
        return Map.of("return_code", returnCode, "return_message", message != null ? message : "");
    }

    private Map<String, String> vnpayResponse(String rspCode, String message) {
        return Map.of("RspCode", rspCode, "Message", message);
    }

    private InvoicePackagePurchaseDTO handleMomoCallback(Map<String, ?> payload, String source) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("MoMo không gửi dữ liệu thanh toán");
        }
        if (!momoPaymentService.verifyCallbackSignature(payload)) {
            throw new IllegalArgumentException("Chữ ký MoMo không hợp lệ");
        }

        String orderId = momoPaymentService.value(payload, "orderId");
        if (orderId.isBlank()) {
            throw new IllegalArgumentException("MoMo không gửi orderId");
        }

        InvoicePackagePurchaseEntity purchase = purchaseRepository.findByPaymentCode(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch MoMo: " + orderId));
        CompanyEntity company = companyRepository.findById(purchase.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công ty của giao dịch MoMo"));
        validateMomoCallback(purchase, payload);
        applyMomoPayType(purchase, payload);

        if (momoPaymentService.isSuccessResult(payload)) {
            String transId = momoPaymentService.value(payload, "transId");
            String note = "MoMo xác nhận thanh toán thành công"
                    + (!transId.isBlank() ? ", transId=" + transId : "");
            InvoicePackagePurchaseDTO dto = completeSuccessfulPurchase(purchase, company, null, source, note, note);
            dto.setPaymentMessage(note);
            return dto;
        }

        String message = "MoMo thanh toán không thành công: resultCode="
                + momoPaymentService.value(payload, "resultCode")
                + ", message=" + momoPaymentService.value(payload, "message");
        InvoicePackagePurchaseDTO dto = failMomoPurchase(purchase, company, message);
        dto.setPaymentMessage(message);
        return dto;
    }

    private void validateMomoCallback(InvoicePackagePurchaseEntity purchase, Map<String, ?> payload) {
        String partnerCode = momoPaymentService.value(payload, "partnerCode");
        if (!momoPaymentService.isExpectedPartner(partnerCode)) {
            throw new IllegalArgumentException("partnerCode MoMo không khớp cấu hình");
        }

        long expectedAmount = amountLong(purchase.getTotalPrice());
        long actualAmount = parseLong(momoPaymentService.value(payload, "amount"), -1);
        if (actualAmount != expectedAmount) {
            throw new IllegalArgumentException("Số tiền MoMo không khớp giao dịch");
        }
    }

    private void applyMomoPayType(InvoicePackagePurchaseEntity purchase, Map<String, ?> payload) {
        String currentMethod = purchase.getPaymentMethod() != null
                ? purchase.getPaymentMethod().trim().toUpperCase(Locale.ROOT)
                : "";
        if (!"MOMO".equals(currentMethod)) {
            return;
        }

        String actualMethod = momoPaymentMethodFromPayType(momoPaymentService.value(payload, "payType"));
        if (actualMethod != null) {
            purchase.setPaymentMethod(actualMethod);
        }
    }

    private String momoPaymentMethodFromPayType(String payType) {
        String normalized = payType != null
                ? payType.trim().toLowerCase(Locale.ROOT).replace("-", "_")
                : "";
        return switch (normalized) {
            case "credit" -> "MOMO_CREDIT";
            case "napas" -> "MOMO_ATM";
            case "qr", "webapp", "web_app", "app", "miniapp", "mini_app", "aio_qr", "banktransfer_qr" -> "MOMO_WALLET";
            case "vts", "paylater", "pay_later" -> "MOMO_PAY_LATER";
            default -> null;
        };
    }

    private InvoicePackagePurchaseDTO completeSuccessfulPurchase(InvoicePackagePurchaseEntity purchase,
                                                                 CompanyEntity company,
                                                                 UserEntity user,
                                                                 String source,
                                                                 String historyNote,
                                                                 String purchaseNote) {
        if ("SUCCESS".equals(purchase.getPaymentStatus()) && purchase.getBuyInvoiceId() != null) {
            return dtoWithInvoiceState(purchase, company);
        }

        purchase.setPaymentStatus("SUCCESS");
        purchase.setPaidAt(purchase.getPaidAt() != null ? purchase.getPaidAt() : LocalDateTime.now());
        purchase.setNote(purchaseNote);

        BuyInvoiceEntity beforeBuyInvoice = findCurrentBuyInvoice(company.getId()).map(this::snapshot).orElse(null);
        BuyInvoiceEntity buyInvoice = upsertBuyInvoice(company.getId(), purchase.getInvoiceQuantity());
        purchase.setBuyInvoiceId(buyInvoice.getId());
        purchase = purchaseRepository.save(purchase);

        buyInvoiceHistoryService.record(
                beforeBuyInvoice,
                snapshot(buyInvoice),
                "PACKAGE_PURCHASE",
                source,
                user,
                purchase.getId(),
                purchase.getPackageName(),
                purchase.getPaymentCode(),
                historyNote
        );

        if (Integer.valueOf(2).equals(company.getStatus())) {
            company.setStatus(1);
            companyRepository.save(company);
        }

        enqueuePurchaseMail(purchase, buyInvoice, company, user);
        return dtoWithInvoiceState(purchase, company);
    }

    private InvoicePackagePurchaseDTO failMomoPurchase(InvoicePackagePurchaseEntity purchase, CompanyEntity company,
                                                       String message) {
        if (!"SUCCESS".equals(purchase.getPaymentStatus())) {
            purchase.setPaymentStatus("FAILED");
            purchase.setNote(message);
            purchase = purchaseRepository.save(purchase);
        }
        return dtoWithInvoiceState(purchase, company);
    }

    private InvoicePackagePurchaseDTO dtoWithInvoiceState(InvoicePackagePurchaseEntity purchase, CompanyEntity company) {
        InvoicePackagePurchaseDTO dto = toPurchaseDto(purchase);
        dto.setCompanyStatus(company != null ? company.getStatus() : null);
        Optional<BuyInvoiceEntity> buyInvoice = findCurrentBuyInvoice(purchase.getCompanyId());
        dto.setTotalInvoices(buyInvoice.map(BuyInvoiceEntity::getAmount).map(this::valueOrZero).orElse(0));
        dto.setUsedInvoices(buyInvoice.map(BuyInvoiceEntity::getAmountUsed).map(this::valueOrZero).orElse(0));
        dto.setRemainingInvoices(valueOrZero(dto.getTotalInvoices()) - valueOrZero(dto.getUsedInvoices()));
        return dto;
    }

    private String buildMomoOrderId(Long purchaseId) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return "MOMO" + purchaseId + time + random;
    }

    private String buildVnpayTxnRef(Long purchaseId) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return "VNPAY" + purchaseId + time + random;
    }

    private String buildZaloPayAppTransId(Long purchaseId) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        String date = now.format(DateTimeFormatter.ofPattern("yyMMdd"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
        String value = date + "_HD" + purchaseId + random;
        return value.length() <= 40 ? value : value.substring(0, 40);
    }

    private String buildMomoRequestId(Long purchaseId) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return "REQ" + purchaseId + time + random;
    }

    private String buildMomoExtraData(InvoicePackagePurchaseEntity purchase) {
        String json = "{\"purchaseId\":\"" + purchase.getId() + "\",\"companyId\":\"" + purchase.getCompanyId() + "\"}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String buildZaloPayAppUser(InvoicePackagePurchaseEntity purchase) {
        String raw = firstNotBlank(
                purchase.getBuyerPhone(),
                purchase.getBuyerEmail(),
                "company_" + purchase.getCompanyId()
        );
        String normalized = raw != null ? raw.replaceAll("\\s+", "_") : "hoadon";
        return normalized.length() <= 50 ? normalized : normalized.substring(0, 50);
    }

    private String buildZaloPayEmbedData(InvoicePackagePurchaseEntity purchase, String redirectUrl) {
        Map<String, Object> embedData = new LinkedHashMap<>();
        embedData.put("redirecturl", redirectUrl);
        embedData.put("preferred_payment_method", zaloPayPreferredPaymentMethods());
        embedData.put("merchantinfo", "purchaseId=" + purchase.getId() + ";companyId=" + purchase.getCompanyId());
        try {
            return objectMapper.writeValueAsString(embedData);
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể tạo embed_data ZaloPay", e);
        }
    }

    private List<String> zaloPayPreferredPaymentMethods() {
        List<String> methods = new ArrayList<>();
        String configured = zaloPayProperties.getPreferredPaymentMethods();
        if (configured != null) {
            for (String part : configured.split(",")) {
                String method = part != null ? part.trim() : "";
                if (!method.isEmpty()) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    private String buildZaloPayItems(InvoicePackagePurchaseEntity purchase) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("itemid", "invoice-package-" + purchase.getPackageId());
        item.put("itemname", nullToBlank(purchase.getPackageName()));
        item.put("itemprice", amountLong(purchase.getTotalPrice()));
        item.put("itemquantity", 1);
        try {
            return objectMapper.writeValueAsString(List.of(item));
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể tạo item ZaloPay", e);
        }
    }

    private String buildAsciiOrderInfo(InvoicePackagePurchaseEntity purchase) {
        String raw = "Thanh toán gói hóa đơn " + nullToBlank(purchase.getPackageName())
                + " mã " + nullToBlank(purchase.getPaymentCode());
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized)
                .replaceAll("")
                .replaceAll("[^A-Za-z0-9 .:_-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String currentRequestIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
            if (request.getRemoteAddr() != null && !request.getRemoteAddr().isBlank()) {
                return request.getRemoteAddr();
            }
        }
        return "127.0.0.1";
    }

    private long amountLong(BigDecimal value) {
        return money(value).longValue();
    }

    private long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String buildFrontendMomoRedirect(String status, String orderId, String message) {
        return buildFrontendPaymentRedirect("momoStatus", status, orderId, message);
    }

    private String buildFrontendPaymentRedirect(String statusParam, String status, String orderId, String message) {
        return trimTrailingSlash(frontendUrl)
                + "/invoice-packages?" + statusParam + "=" + encodeUrlParam(status)
                + "&orderId=" + encodeUrlParam(orderId)
                + "&message=" + encodeUrlParam(message);
    }

    private String encodeUrlParam(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    private String trimTrailingSlash(String value) {
        String normalized = firstNotBlank(value);
        while (normalized != null && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized != null ? normalized : "";
    }

    private record PurchaseContext(
            InvoicePackageEntity invoicePackage,
            CompanyEntity company,
            UserEntity user,
            String method
    ) {}

    @Override
    public Page<InvoicePackagePurchaseDTO> listPurchases(InvoicePackagePurchaseFilterDTO filter, Pageable pageable) {
        return purchaseRepository.findAll(purchaseSpec(filter, false), pageable).map(this::toPurchaseDto);
    }

    @Override
    public Page<InvoicePackagePurchaseDTO> listMyPurchases(Long companyId, InvoicePackagePurchaseFilterDTO filter, Pageable pageable) {
        InvoicePackagePurchaseFilterDTO normalized = filter != null ? filter : new InvoicePackagePurchaseFilterDTO();
        normalized.setCompanyId(companyId);
        return purchaseRepository.findAll(purchaseSpec(normalized, false), pageable).map(this::toPurchaseDto);
    }

    @Override
    public InvoicePackageStatisticsDTO statistics(InvoicePackagePurchaseFilterDTO filter) {
        InvoicePackagePurchaseFilterDTO normalized = normalizeStatsFilter(filter);
        List<InvoicePackagePurchaseEntity> purchases = purchaseRepository.findAll(
                purchaseSpec(normalized, true),
                Sort.by("createdAt").ascending()
        );

        InvoicePackageStatisticsDTO stats = new InvoicePackageStatisticsDTO();
        stats.setTotalOrders((long) purchases.size());

        int totalInvoices = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<String, InvoicePackageMonthlyStatisticDTO> byMonth = initMonthMap(normalized);

        for (InvoicePackagePurchaseEntity purchase : purchases) {
            int quantity = valueOrZero(purchase.getInvoiceQuantity());
            BigDecimal revenue = purchase.getTotalPrice() != null ? purchase.getTotalPrice() : BigDecimal.ZERO;
            totalInvoices += quantity;
            totalRevenue = totalRevenue.add(revenue);

            LocalDateTime time = purchase.getPaidAt() != null ? purchase.getPaidAt() : purchase.getCreatedAt();
            String month = time != null ? time.format(DateTimeFormatter.ofPattern("yyyy-MM")) : YearMonth.now().toString();
            InvoicePackageMonthlyStatisticDTO item = byMonth.computeIfAbsent(month, key -> emptyMonth(key));
            item.setOrderCount(valueOrZero(item.getOrderCount()) + 1);
            item.setInvoiceQuantity(valueOrZero(item.getInvoiceQuantity()) + quantity);
            item.setRevenue(item.getRevenue().add(revenue));
        }

        stats.setTotalInvoices(totalInvoices);
        stats.setTotalRevenue(totalRevenue);
        stats.setMonthly(new ArrayList<>(byMonth.values()));
        return stats;
    }

    @Override
    public byte[] exportPurchases(InvoicePackagePurchaseFilterDTO filter) {
        List<InvoicePackagePurchaseEntity> purchases = purchaseRepository.findAll(
                purchaseSpec(filter, false),
                Sort.by("createdAt").descending()
        );

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bao cao mua goi");
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

            String[] headers = {
                    "STT", "Mã giao dịch", "Công ty", "Mã số thuế", "Gói hóa đơn",
                    "Số hóa đơn", "Đơn giá", "Thành tiền", "Thanh toán", "Trạng thái", "Ngày thanh toán"
            };
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowIdx = 1;
            for (int i = 0; i < purchases.size(); i++) {
                InvoicePackagePurchaseEntity purchase = purchases.get(i);
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(nullToBlank(purchase.getPaymentCode()));
                row.createCell(2).setCellValue(nullToBlank(purchase.getCompanyName()));
                row.createCell(3).setCellValue(nullToBlank(purchase.getCompanyTaxcode()));
                row.createCell(4).setCellValue(nullToBlank(purchase.getPackageName()));
                row.createCell(5).setCellValue(valueOrZero(purchase.getInvoiceQuantity()));
                row.createCell(6).setCellValue(toDouble(purchase.getUnitPrice()));
                row.getCell(6).setCellStyle(moneyStyle);
                row.createCell(7).setCellValue(toDouble(purchase.getTotalPrice()));
                row.getCell(7).setCellStyle(moneyStyle);
                row.createCell(8).setCellValue(paymentMethodLabel(purchase.getPaymentMethod()));
                row.createCell(9).setCellValue("SUCCESS".equals(purchase.getPaymentStatus()) ? "Thành công" : nullToBlank(purchase.getPaymentStatus()));
                row.createCell(10).setCellValue(purchase.getPaidAt() != null ? purchase.getPaidAt().format(formatter) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể xuất báo cáo Excel", e);
        }
    }

    private Specification<InvoicePackageEntity> packageSpec(InvoicePackageFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter != null) {
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }
                if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                    String like = "%" + filter.getKeyword().trim() + "%";
                    predicates.add(cb.like(root.get("name"), like));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<InvoicePackagePurchaseEntity> purchaseSpec(InvoicePackagePurchaseFilterDTO filter, boolean onlySuccessWhenBlank) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter != null) {
                if (filter.getCompanyId() != null) {
                    predicates.add(cb.equal(root.get("companyId"), filter.getCompanyId()));
                }
                if (filter.getPaymentMethod() != null && !filter.getPaymentMethod().isBlank()) {
                    String method = filter.getPaymentMethod().trim().toUpperCase(Locale.ROOT);
                    if ("MOMO".equals(method)) {
                        predicates.add(cb.like(root.get("paymentMethod"), "MOMO%"));
                    } else {
                        predicates.add(cb.equal(root.get("paymentMethod"), method));
                    }
                }
                if (filter.getPaymentStatus() != null && !filter.getPaymentStatus().isBlank()) {
                    predicates.add(cb.equal(root.get("paymentStatus"), filter.getPaymentStatus().trim().toUpperCase(Locale.ROOT)));
                } else if (onlySuccessWhenBlank) {
                    predicates.add(cb.equal(root.get("paymentStatus"), "SUCCESS"));
                }
                if (filter.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate().atStartOfDay()));
                }
                if (filter.getToDate() != null) {
                    predicates.add(cb.lessThan(root.get("createdAt"), filter.getToDate().plusDays(1).atStartOfDay()));
                }
                if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                    String like = "%" + filter.getKeyword().trim() + "%";
                    predicates.add(cb.or(
                            cb.like(root.get("companyName"), like),
                            cb.like(root.get("companyTaxcode"), like),
                            cb.like(root.get("packageName"), like),
                            cb.like(root.get("paymentCode"), like)
                    ));
                }
            } else if (onlySuccessWhenBlank) {
                predicates.add(cb.equal(root.get("paymentStatus"), "SUCCESS"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private BuyInvoiceEntity upsertBuyInvoice(Long companyId, Integer invoiceQuantity) {
        BuyInvoiceEntity buyInvoice = findCurrentBuyInvoice(companyId).orElseGet(BuyInvoiceEntity::new);
        if (buyInvoice.getId() == null) {
            buyInvoice.setCompanyId(companyId);
            buyInvoice.setCreatedAt(LocalDateTime.now());
            buyInvoice.setAmount(0);
            buyInvoice.setAmountUsed(0);
        }
        buyInvoice.setStatus(1);
        buyInvoice.setAmount(valueOrZero(buyInvoice.getAmount()) + valueOrZero(invoiceQuantity));
        buyInvoice.setAmountUsed(valueOrZero(buyInvoice.getAmountUsed()));
        buyInvoice.setUpdatedAt(LocalDateTime.now());
        return buyInvoiceRepository.save(buyInvoice);
    }

    private Optional<BuyInvoiceEntity> findCurrentBuyInvoice(Long companyId) {
        Optional<BuyInvoiceEntity> active = buyInvoiceRepository.findFirstByCompanyIdAndStatusOrderByIdDesc(companyId, 1);
        return active.isPresent() ? active : buyInvoiceRepository.findFirstByCompanyIdOrderByIdDesc(companyId);
    }

    private BuyInvoiceEntity snapshot(BuyInvoiceEntity source) {
        if (source == null) return null;
        BuyInvoiceEntity copy = new BuyInvoiceEntity();
        copy.setId(source.getId());
        copy.setCompanyId(source.getCompanyId());
        copy.setAmount(source.getAmount());
        copy.setAmountUsed(source.getAmountUsed());
        copy.setStatus(source.getStatus());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    private void enqueuePurchaseMail(InvoicePackagePurchaseEntity purchase, BuyInvoiceEntity buyInvoice,
                                     CompanyEntity company, UserEntity user) {
        String toEmail = firstNotBlank(company.getContactMail(), company.getEmail(), user != null ? user.getEmail() : null);
        if (toEmail == null) {
            return;
        }
        try {
            ensureBuyInvoiceMailTemplate(company);
            Map<String, String> vars = new HashMap<>();
            vars.put("SUBJECT", "Thông báo mua gói hóa đơn thành công");
            vars.put("NAME", firstNotBlank(purchase.getBuyerName(), company.getContactName(), company.getName(), user != null ? user.getUsername() : null, ""));
            vars.put("COMPANY", nullToBlank(company.getName()));
            vars.put("PACKAGE_NAME", nullToBlank(purchase.getPackageName()));
            vars.put("INVOICE_QUANTITY", String.valueOf(valueOrZero(purchase.getInvoiceQuantity())));
            vars.put("UNIT_PRICE", formatMoney(purchase.getUnitPrice()));
            vars.put("AMOUNT", formatMoney(purchase.getTotalPrice()));
            vars.put("TOTAL_PRICE", formatMoney(purchase.getTotalPrice()));
            vars.put("PAYMENT_METHOD", nullToBlank(purchase.getPaymentMethod()));
            vars.put("PAYMENT_CODE", nullToBlank(purchase.getPaymentCode()));
            vars.put("PAID_AT", purchase.getPaidAt() != null ? purchase.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
            vars.put("TOTAL_INVOICES", String.valueOf(valueOrZero(buyInvoice.getAmount())));
            vars.put("USED_INVOICES", String.valueOf(valueOrZero(buyInvoice.getAmountUsed())));
            vars.put("REMAINING_INVOICES", String.valueOf(valueOrZero(buyInvoice.getAmount()) - valueOrZero(buyInvoice.getAmountUsed())));
            vars.put("HTML_BODY", fallbackMailBody(vars));

            MailJobMessage message = new MailJobMessage();
            message.setTemplateKey(MAIL_KEY);
            message.setCompanyId(company.getId());
            message.setToEmail(toEmail);
            message.setToName(vars.get("NAME"));
            message.setVariables(vars);
            mailQueueService.enqueue(message);
        } catch (Exception e) {
            log.warn("Không thể đưa mail mua gói hóa đơn vào hàng đợi: {}", e.getMessage());
        }
    }

    private void ensureBuyInvoiceMailTemplate(CompanyEntity company) {
        MailTemplateEntity existing = mailTemplateRepository.findSystemByKey(MAIL_KEY);
        if (existing != null) {
            return;
        }
        CompanyEntity templateCompany = companyRepository.findById(SystemMail.COMPANY_ID)
                .orElse(company);
        MailTemplateEntity template = new MailTemplateEntity();
        template.setCompany(templateCompany);
        template.setKey(MAIL_KEY);
        template.setTitle("Thông báo mua gói hóa đơn thành công");
        template.setStatus((byte) 1);
        template.setSystem((byte) 1);
        template.setContent(defaultMailTemplate());
        mailTemplateRepository.save(template);
    }

    private String defaultMailTemplate() {
        return """
                <div style="background:#eeeeee;padding:24px 0;font-family:Roboto,Helvetica,Arial,sans-serif;color:#212b35">
                  <div style="background:#ffffff;margin:0 auto;max-width:600px;padding:28px">
                    <p style="font-size:16px;margin:0 0 16px">Xin chào [NAME],</p>
                    <p style="font-size:14px;line-height:24px;margin:0 0 18px">Hệ thống đã ghi nhận giao dịch mua gói hóa đơn của quý khách.</p>
                    <table style="width:100%;border-collapse:collapse;font-size:14px;line-height:22px">
                      <tr><td style="padding:8px;border-bottom:1px solid #edf2f7">Công ty</td><td style="padding:8px;border-bottom:1px solid #edf2f7"><strong>[COMPANY]</strong></td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #edf2f7">Gói hóa đơn</td><td style="padding:8px;border-bottom:1px solid #edf2f7">[PACKAGE_NAME]</td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #edf2f7">Số hóa đơn</td><td style="padding:8px;border-bottom:1px solid #edf2f7">[INVOICE_QUANTITY]</td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #edf2f7">Tổng thanh toán</td><td style="padding:8px;border-bottom:1px solid #edf2f7"><strong>[TOTAL_PRICE]</strong></td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #edf2f7">Phương thức</td><td style="padding:8px;border-bottom:1px solid #edf2f7">[PAYMENT_METHOD]</td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #edf2f7">Mã giao dịch</td><td style="padding:8px;border-bottom:1px solid #edf2f7">[PAYMENT_CODE]</td></tr>
                    </table>
                    <p style="font-size:14px;line-height:24px;margin:18px 0 0">Số hóa đơn còn lại hiện tại: <strong>[REMAINING_INVOICES]</strong>.</p>
                    <p style="font-size:14px;line-height:24px;margin:18px 0 0">Đây là mail tự động từ hệ thống. Vui lòng không trả lời mail này.</p>
                    <p style="font-size:14px;margin:18px 0 0">Trân trọng!</p>
                  </div>
                  <div style="margin:0 auto;max-width:600px;padding:12px 28px;text-align:center;color:#555;font-size:12px">Giải Pháp Hóa Đơn Điện Tử Thông Minh được cung cấp bởi P.A Việt Nam</div>
                </div>
                """;
    }

    private String fallbackMailBody(Map<String, String> vars) {
        return "<p>Xin chào " + vars.get("NAME") + ",</p>"
                + "<p>Quý khách đã mua thành công gói " + vars.get("PACKAGE_NAME")
                + " với số lượng " + vars.get("INVOICE_QUANTITY") + " hóa đơn.</p>";
    }

    private void ensureDefaultPackage() {
        ensureBuyInvoiceMailTemplateIfPossible();
        if (packageRepository.count() > 0) {
            return;
        }
        InvoicePackageEntity sample = new InvoicePackageEntity();
        sample.setName("Bill #1");
        sample.setInvoiceQuantity(500);
        sample.setUnitPrice(BigDecimal.valueOf(600));
        sample.setTotalPrice(BigDecimal.valueOf(300000));
        sample.setIncludeTrial(true);
        sample.setDescription("Bao gồm gói trải nghiệm");
        sample.setStatus(1);
        sample.setDisplayOrder(1);
        packageRepository.save(sample);
    }

    private void ensureBuyInvoiceMailTemplateIfPossible() {
        if (mailTemplateRepository.findSystemByKey(MAIL_KEY) != null) {
            return;
        }
        List<CompanyEntity> companies = companyRepository.findAll(PageRequest.of(0, 1)).getContent();
        if (!companies.isEmpty()) {
            ensureBuyInvoiceMailTemplate(companies.get(0));
        }
    }

    private InvoicePackageResponseDTO toPackageDto(InvoicePackageEntity entity) {
        InvoicePackageResponseDTO dto = new InvoicePackageResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setInvoiceQuantity(entity.getInvoiceQuantity());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setTotalPrice(entity.getTotalPrice());
        dto.setIncludeTrial(entity.getIncludeTrial());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private InvoicePackagePurchaseDTO toPurchaseDto(InvoicePackagePurchaseEntity entity) {
        InvoicePackagePurchaseDTO dto = new InvoicePackagePurchaseDTO();
        dto.setId(entity.getId());
        dto.setPackageId(entity.getPackageId());
        dto.setPackageName(entity.getPackageName());
        dto.setCompanyId(entity.getCompanyId());
        dto.setCompanyName(entity.getCompanyName());
        dto.setCompanyTaxcode(entity.getCompanyTaxcode());
        dto.setBuyerName(entity.getBuyerName());
        dto.setBuyerEmail(entity.getBuyerEmail());
        dto.setBuyerPhone(entity.getBuyerPhone());
        dto.setInvoiceQuantity(entity.getInvoiceQuantity());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setTotalPrice(entity.getTotalPrice());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setPaymentStatus(entity.getPaymentStatus());
        dto.setPaymentCode(entity.getPaymentCode());
        dto.setBuyInvoiceId(entity.getBuyInvoiceId());
        dto.setPaidAt(entity.getPaidAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private InvoicePackagePurchaseFilterDTO normalizeStatsFilter(InvoicePackagePurchaseFilterDTO filter) {
        InvoicePackagePurchaseFilterDTO normalized = filter != null ? filter : new InvoicePackagePurchaseFilterDTO();
        if (normalized.getFromDate() == null && normalized.getToDate() == null) {
            LocalDate now = LocalDate.now();
            normalized.setFromDate(LocalDate.of(now.getYear(), 1, 1));
            normalized.setToDate(LocalDate.of(now.getYear(), 12, 31));
        }
        return normalized;
    }

    private Map<String, InvoicePackageMonthlyStatisticDTO> initMonthMap(InvoicePackagePurchaseFilterDTO filter) {
        LocalDate from = filter.getFromDate() != null ? filter.getFromDate() : LocalDate.now().withDayOfYear(1);
        LocalDate to = filter.getToDate() != null ? filter.getToDate() : LocalDate.now();
        YearMonth start = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        Map<String, InvoicePackageMonthlyStatisticDTO> map = new LinkedHashMap<>();
        YearMonth cursor = start;
        while (!cursor.isAfter(end)) {
            map.put(cursor.toString(), emptyMonth(cursor.toString()));
            cursor = cursor.plusMonths(1);
        }
        return map;
    }

    private InvoicePackageMonthlyStatisticDTO emptyMonth(String month) {
        InvoicePackageMonthlyStatisticDTO item = new InvoicePackageMonthlyStatisticDTO();
        item.setMonth(month);
        item.setOrderCount(0L);
        item.setInvoiceQuantity(0);
        item.setRevenue(BigDecimal.ZERO);
        return item;
    }

    private BigDecimal resolveTotalPrice(InvoicePackageRequestDTO dto) {
        if (dto.getTotalPrice() != null && dto.getTotalPrice().compareTo(BigDecimal.ZERO) >= 0) {
            return money(dto.getTotalPrice());
        }
        return money(dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getInvoiceQuantity())));
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String method = paymentMethod != null ? paymentMethod.trim().toUpperCase(Locale.ROOT) : "";
        if (!isMomoPaymentMethod(method) && !"VNPAY".equals(method) && !"ZALOPAY".equals(method)) {
            return "MOMO";
        }
        return method;
    }

    private boolean isMomoPaymentMethod(String method) {
        String normalized = method != null ? method.trim().toUpperCase(Locale.ROOT) : "";
        return "MOMO".equals(normalized)
                || "MOMO_WALLET".equals(normalized)
                || "MOMO_ATM".equals(normalized)
                || "MOMO_CREDIT".equals(normalized)
                || "MOMO_PAY_LATER".equals(normalized);
    }

    private String momoRequestType(String paymentMethod) {
        String method = paymentMethod != null ? paymentMethod.trim().toUpperCase(Locale.ROOT) : "";
        return switch (method) {
            case "MOMO_WALLET" -> "captureWallet";
            case "MOMO_ATM" -> "payWithATM";
            case "MOMO_CREDIT" -> "payWithCC";
            case "MOMO_PAY_LATER" -> "payWithVTS";
            default -> "payWithMethod";
        };
    }

    private String momoPaymentLabel(String paymentMethod) {
        String method = paymentMethod != null ? paymentMethod.trim().toUpperCase(Locale.ROOT) : "";
        return switch (method) {
            case "MOMO_WALLET" -> "MoMo ví điện tử";
            case "MOMO_ATM" -> "MoMo ATM nội địa";
            case "MOMO_CREDIT" -> "MoMo thẻ quốc tế";
            case "MOMO_PAY_LATER" -> "MoMo trả sau";
            default -> "MoMo";
        };
    }

    private String paymentMethodLabel(String paymentMethod) {
        String method = paymentMethod != null ? paymentMethod.trim().toUpperCase(Locale.ROOT) : "";
        if ("VNPAY".equals(method)) {
            return "VNPAY";
        }
        if ("ZALOPAY".equals(method)) {
            return "ZaloPay";
        }
        if (isMomoPaymentMethod(method)) {
            return momoPaymentLabel(method);
        }
        return nullToBlank(paymentMethod);
    }

    private Map<String, Object> momoUserInfo(InvoicePackagePurchaseEntity purchase, String paymentMethod) {
        String method = paymentMethod != null ? paymentMethod.trim().toUpperCase(Locale.ROOT) : "";
        if ("MOMO_ATM".equals(method)) {
            return Map.of();
        }

        Map<String, Object> userInfo = new LinkedHashMap<>();
        String name = firstNotBlank(purchase.getBuyerName(), purchase.getCompanyName());
        String phone = firstNotBlank(purchase.getBuyerPhone());
        String email = firstNotBlank(purchase.getBuyerEmail());
        if ("MOMO_CREDIT".equals(method) && email == null) {
            throw new IllegalArgumentException("Thanh toán MoMo thẻ quốc tế cần email người mua");
        }
        if (name != null) {
            userInfo.put("name", name);
        }
        if (phone != null) {
            userInfo.put("phoneNumber", phone);
        }
        if (email != null) {
            userInfo.put("email", email);
        }
        return userInfo;
    }

    private String fakePaymentCode(String method) {
        return method + "-FAKE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private String formatMoney(BigDecimal value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return nf.format(value != null ? value : BigDecimal.ZERO);
    }

    private int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }

    private long valueOrZero(Long value) {
        return value != null ? value : 0L;
    }

    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0D;
    }

    private String resolveBuyerName(UserEntity user, CompanyEntity company) {
        return firstNotBlank(user.getName(), company.getContactName(), company.getName(), user.getUsername());
    }

    private String resolveBuyerEmail(UserEntity user, CompanyEntity company) {
        return firstNotBlank(user.getEmail(), company.getContactMail(), company.getEmail());
    }

    private String resolveBuyerPhone(UserEntity user, CompanyEntity company) {
        return firstNotBlank(user.getPhone(), company.getContactPhone(), company.getHotline());
    }

    private String firstNotBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String nullToBlank(String value) {
        return value != null ? value : "";
    }
}
