package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.hoadon.config.VnpayProperties;
import vn.hoadon.services.VnpayPaymentService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
public class VnpayPaymentServiceImpl implements VnpayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(VnpayPaymentServiceImpl.class);
    private static final String VERSION = "2.1.0";
    private static final String COMMAND = "pay";
    private static final String CURRENCY = "VND";
    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties properties;

    @Value("${app.backend-url:http://localhost:8081}")
    private String backendUrl;

    public VnpayPaymentServiceImpl(VnpayProperties properties) {
        this.properties = properties;
    }

    @Override
    public CreatePaymentResponse createPaymentUrl(CreatePaymentRequest request) {
        ensureConfigured();

        LocalDateTime now = LocalDateTime.now();
        String returnUrl = firstNotBlank(request.returnUrl(), properties.getReturnUrl(),
                trimTrailingSlash(backendUrl) + "/v1/invoice-packages/vnpay/return");

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", VERSION);
        params.put("vnp_Command", COMMAND);
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put("vnp_Amount", String.valueOf(request.amount() * 100));
        params.put("vnp_CreateDate", now.format(VNPAY_TIME));
        params.put("vnp_CurrCode", CURRENCY);
        params.put("vnp_IpAddr", normalizeIpAddress(request.ipAddress()));
        params.put("vnp_Locale", firstNotBlank(properties.getLocale(), "vn"));
        params.put("vnp_OrderInfo", firstNotBlank(request.orderInfo(), "Thanh toan hoa don"));
        params.put("vnp_OrderType", firstNotBlank(properties.getOrderType(), "other"));
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_TxnRef", request.txnRef());
        params.put("vnp_ExpireDate", now.plusMinutes(Math.max(properties.getExpireMinutes(), 1)).format(VNPAY_TIME));

        SignedQuery signedQuery = buildSignedQuery(params);
        String secureHash = hmacSha512(signedQuery.hashData());
        log.debug(
                "VNPAY payment url: txnRef={}, tmnCode={}, amount={}, returnUrl={}, hashData={}, secureHash={}, secretTail={}",
                request.txnRef(),
                properties.getTmnCode(),
                request.amount(),
                returnUrl,
                signedQuery.hashData(),
                secureHash,
                secretTail()
        );
        String query = signedQuery.query() + "&vnp_SecureHash=" + secureHash;
        return new CreatePaymentResponse(trimTrailingSlash(properties.getPayUrl()) + "?" + query, "Đã tạo URL thanh toán VNPAY");
    }

    @Override
    public boolean verifyCallbackSignature(Map<String, ?> payload) {
        ensureConfigured();
        String actual = value(payload, "vnp_SecureHash");
        if (actual.isBlank()) {
            return false;
        }

        Map<String, String> signedParams = new TreeMap<>();
        if (payload != null) {
            for (Map.Entry<String, ?> entry : payload.entrySet()) {
                String key = entry.getKey();
                if (key == null || !key.startsWith("vnp_")
                        || "vnp_SecureHash".equals(key)
                        || "vnp_SecureHashType".equals(key)) {
                    continue;
                }
                String val = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
                if (!val.isBlank()) {
                    signedParams.put(key, val);
                }
            }
        }

        String expected = hmacSha512(buildHashData(signedParams));
        return MessageDigest.isEqual(
                expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                actual.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public boolean isSuccessResult(Map<String, ?> payload) {
        return "00".equals(value(payload, "vnp_ResponseCode"))
                && "00".equals(value(payload, "vnp_TransactionStatus"));
    }

    @Override
    public boolean isExpectedTmnCode(String tmnCode) {
        return !isBlank(tmnCode) && tmnCode.trim().equals(properties.getTmnCode());
    }

    @Override
    public String value(Map<String, ?> payload, String key) {
        if (payload == null || key == null) {
            return "";
        }
        Object value = payload.get(key);
        return value != null ? String.valueOf(value) : "";
    }

    private void ensureConfigured() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Thanh toán VNPAY đang tắt cấu hình");
        }
        if (isBlank(properties.getTmnCode()) || isBlank(properties.getHashSecret())) {
            throw new IllegalStateException("Chưa cấu hình vnpay.tmn-code và vnpay.hash-secret");
        }
    }

    private SignedQuery buildSignedQuery(Map<String, String> params) {
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!hashData.isEmpty()) {
                hashData.append('&');
                query.append('&');
            }
            hashData.append(entry.getKey()).append('=').append(encode(value));
            query.append(encode(entry.getKey())).append('=').append(encode(value));
        }
        return new SignedQuery(hashData.toString(), query.toString());
    }

    private String buildHashData(Map<String, String> params) {
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!hashData.isEmpty()) {
                hashData.append('&');
            }
            hashData.append(entry.getKey()).append('=').append(encode(value));
        }
        return hashData.toString();
    }

    private String hmacSha512(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(properties.getHashSecret().trim().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo chữ ký VNPAY", e);
        }
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value != null ? value : "", StandardCharsets.US_ASCII.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Không thể mã hóa tham số VNPAY", e);
        }
    }

    private String normalizeIpAddress(String value) {
        String ip = firstNotBlank(value, "127.0.0.1");
        int commaIndex = ip.indexOf(',');
        if (commaIndex >= 0) {
            ip = ip.substring(0, commaIndex).trim();
        }
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    private String secretTail() {
        String secret = firstNotBlank(properties.getHashSecret());
        if (secret.length() <= 4) {
            return "****";
        }
        return "****" + secret.substring(secret.length() - 4);
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimTrailingSlash(String value) {
        String normalized = firstNotBlank(value);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private record SignedQuery(String hashData, String query) {}
}
