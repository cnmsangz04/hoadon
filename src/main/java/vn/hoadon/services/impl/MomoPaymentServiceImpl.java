package vn.hoadon.services.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.hoadon.config.MomoProperties;
import vn.hoadon.services.MomoPaymentService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class MomoPaymentServiceImpl implements MomoPaymentService {

    private static final String DEFAULT_REQUEST_TYPE = "payWithMethod";
    private static final String CREATE_PATH = "/v2/gateway/api/create";

    private final MomoProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.backend-url:http://localhost:8081}")
    private String backendUrl;

    public MomoPaymentServiceImpl(MomoProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        ensureConfigured();

        String redirectUrl = firstNotBlank(request.redirectUrl(), properties.getRedirectUrl(),
                trimTrailingSlash(backendUrl) + "/v1/invoice-packages/momo/return");
        String ipnUrl = firstNotBlank(request.ipnUrl(), properties.getIpnUrl(),
                trimTrailingSlash(backendUrl) + "/v1/invoice-packages/momo/ipn");
        String extraData = request.extraData() != null ? request.extraData() : "";
        String orderInfo = request.orderInfo() != null ? request.orderInfo() : "";
        String requestType = firstNotBlank(request.requestType(), DEFAULT_REQUEST_TYPE);

        String rawSignature = "accessKey=" + properties.getAccessKey()
                + "&amount=" + request.amount()
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + request.orderId()
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + properties.getPartnerCode()
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + request.requestId()
                + "&requestType=" + requestType;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", properties.getPartnerCode());
        body.put("requestType", requestType);
        body.put("ipnUrl", ipnUrl);
        body.put("redirectUrl", redirectUrl);
        body.put("orderId", request.orderId());
        body.put("amount", request.amount());
        body.put("orderInfo", orderInfo);
        body.put("requestId", request.requestId());
        body.put("extraData", extraData);
        body.put("signature", hmacSha256(rawSignature));
        body.put("lang", firstNotBlank(properties.getLang(), "vi"));
        if (request.userInfo() != null && !request.userInfo().isEmpty()) {
            body.put("userInfo", request.userInfo());
        }

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(trimTrailingSlash(properties.getEndpoint()) + CREATE_PATH))
                    .timeout(Duration.ofSeconds(35))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("MoMo trả HTTP " + response.statusCode());
            }

            Map<String, Object> responseBody = objectMapper.readValue(response.body(), new TypeReference<>() {});
            int resultCode = toInt(responseBody.get("resultCode"), -1);
            String message = value(responseBody, "message");
            if (resultCode != 0) {
                throw new IllegalStateException("MoMo từ chối tạo giao dịch: " + firstNotBlank(message, "resultCode=" + resultCode));
            }

            return new CreatePaymentResponse(
                    value(responseBody, "payUrl"),
                    value(responseBody, "deeplink"),
                    value(responseBody, "qrCodeUrl"),
                    message,
                    resultCode,
                    responseBody
            );
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo giao dịch MoMo: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyCallbackSignature(Map<String, ?> payload) {
        ensureConfigured();
        String actual = value(payload, "signature");
        if (actual.isBlank()) {
            return false;
        }
        String rawSignature = "accessKey=" + properties.getAccessKey()
                + "&amount=" + value(payload, "amount")
                + "&extraData=" + value(payload, "extraData")
                + "&message=" + value(payload, "message")
                + "&orderId=" + value(payload, "orderId")
                + "&orderInfo=" + value(payload, "orderInfo")
                + "&orderType=" + value(payload, "orderType")
                + "&partnerCode=" + value(payload, "partnerCode")
                + "&payType=" + value(payload, "payType")
                + "&requestId=" + value(payload, "requestId")
                + "&responseTime=" + value(payload, "responseTime")
                + "&resultCode=" + value(payload, "resultCode")
                + "&transId=" + value(payload, "transId");
        String expected = hmacSha256(rawSignature);
        return MessageDigest.isEqual(
                expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                actual.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public boolean isSuccessResult(Map<String, ?> payload) {
        return toInt(payload != null ? payload.get("resultCode") : null, -1) == 0;
    }

    @Override
    public boolean isExpectedPartner(String partnerCode) {
        return !isBlank(partnerCode) && partnerCode.equals(properties.getPartnerCode());
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
            throw new IllegalStateException("Thanh toán MoMo đang tắt cấu hình");
        }
        if (isBlank(properties.getPartnerCode()) || isBlank(properties.getAccessKey()) || isBlank(properties.getSecretKey())) {
            throw new IllegalStateException("Chưa cấu hình momo.partner-code, momo.access-key, momo.secret-key");
        }
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo chữ ký MoMo", e);
        }
    }

    private int toInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
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
}
