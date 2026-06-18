package vn.hoadon.services.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import vn.hoadon.config.ZaloPayProperties;
import vn.hoadon.services.ZaloPayPaymentService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
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
public class ZaloPayPaymentServiceImpl implements ZaloPayPaymentService {

    private final ZaloPayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ZaloPayPaymentServiceImpl(ZaloPayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        ensureConfigured();

        String appId = properties.getAppId().trim();
        String appUser = firstNotBlank(request.appUser(), properties.getAppUser(), "hoadon");
        String embedData = firstNotBlank(request.embedData(), "{}");
        String item = firstNotBlank(request.item(), "[]");
        String bankCode = firstNotBlank(request.bankCode(), properties.getBankCode(), "");
        String appTime = String.valueOf(System.currentTimeMillis());
        String data = appId + "|" + request.appTransId() + "|" + appUser + "|" + request.amount()
                + "|" + appTime + "|" + embedData + "|" + item;

        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", appId);
        params.put("app_user", appUser);
        params.put("app_time", appTime);
        params.put("amount", String.valueOf(request.amount()));
        params.put("app_trans_id", request.appTransId());
        params.put("embed_data", embedData);
        params.put("item", item);
        params.put("description", firstNotBlank(request.description(), "Thanh toán gói hóa đơn"));
        params.put("bank_code", bankCode);
        params.put("mac", hmacSha256(properties.getKey1(), data));
        if (!isBlank(request.redirectUrl())) {
            params.put("redirect_url", request.redirectUrl().trim());
        }
        if (!isBlank(request.callbackUrl())) {
            params.put("callback_url", request.callbackUrl().trim());
        }

        Map<String, Object> responseBody = postForm(properties.getCreateOrderUrl(), params, "ZaloPay tạo đơn");
        int returnCode = toInt(firstValue(responseBody, "return_code", "returncode"), -1);
        String message = firstNotBlank(
                value(responseBody, "return_message"),
                value(responseBody, "returnmessage"),
                value(responseBody, "sub_return_message"),
                value(responseBody, "subreturnmessage")
        );
        if (returnCode != 1) {
            throw new IllegalStateException("ZaloPay từ chối tạo giao dịch: " + firstNotBlank(message, "return_code=" + returnCode));
        }

        String payUrl = firstNotBlank(
                value(responseBody, "order_url"),
                value(responseBody, "orderurl"),
                value(responseBody, "orderUrl")
        );
        if (payUrl.isBlank()) {
            throw new IllegalStateException("ZaloPay không trả về URL thanh toán");
        }

        return new CreatePaymentResponse(
                payUrl,
                firstNotBlank(
                        value(responseBody, "zp_trans_token"),
                        value(responseBody, "zptranstoken"),
                        value(responseBody, "order_token")
                ),
                firstNotBlank(message, "Đã tạo giao dịch ZaloPay"),
                returnCode,
                responseBody
        );
    }

    @Override
    public CallbackPayment verifyCallback(Map<String, ?> payload) {
        ensureConfigured();
        String data = value(payload, "data");
        String actual = value(payload, "mac");
        if (data.isBlank() || actual.isBlank()) {
            throw new IllegalArgumentException("ZaloPay không gửi đủ dữ liệu callback");
        }

        String expected = hmacSha256(properties.getKey2(), data);
        if (!constantEquals(expected, actual)) {
            throw new IllegalArgumentException("Chữ ký callback ZaloPay không hợp lệ");
        }

        Map<String, Object> dataMap;
        try {
            dataMap = objectMapper.readValue(data, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Không đọc được dữ liệu callback ZaloPay", e);
        }

        String appId = firstNotBlank(value(dataMap, "app_id"), value(dataMap, "appid"));
        String appTransId = firstNotBlank(value(dataMap, "app_trans_id"), value(dataMap, "apptransid"));
        if (appTransId.isBlank()) {
            throw new IllegalArgumentException("ZaloPay không gửi mã giao dịch");
        }

        return new CallbackPayment(
                appId,
                appTransId,
                parseLong(firstNotBlank(value(dataMap, "amount")), -1),
                firstNotBlank(value(dataMap, "zp_trans_id"), value(dataMap, "zptransid")),
                dataMap
        );
    }

    @Override
    public boolean verifyRedirectChecksum(Map<String, ?> params) {
        ensureConfigured();
        String actual = value(params, "checksum");
        if (actual.isBlank()) {
            return false;
        }

        String data = firstNotBlank(value(params, "app_id"), value(params, "appid"))
                + "|" + firstNotBlank(value(params, "app_trans_id"), value(params, "apptransid"))
                + "|" + firstNotBlank(value(params, "pmc_id"), value(params, "pmcid"))
                + "|" + firstNotBlank(value(params, "bank_code"), value(params, "bankcode"))
                + "|" + value(params, "amount")
                + "|" + firstNotBlank(value(params, "discount_amount"), value(params, "discountamount"))
                + "|" + value(params, "status");
        String expected = hmacSha256(properties.getKey2(), data);
        return constantEquals(expected, actual);
    }

    @Override
    public boolean isSuccessRedirect(Map<String, ?> params) {
        String status = firstNotBlank(value(params, "status"), value(params, "return_code"), value(params, "returncode"));
        return "1".equals(status);
    }

    @Override
    public boolean isExpectedAppId(String appId) {
        return !isBlank(appId) && appId.trim().equals(properties.getAppId());
    }

    @Override
    public QueryStatusResponse queryStatus(String appTransId) {
        ensureConfigured();
        if (isBlank(appTransId)) {
            throw new IllegalArgumentException("Thiếu mã giao dịch ZaloPay");
        }

        String appId = properties.getAppId().trim();
        String data = appId + "|" + appTransId.trim() + "|" + properties.getKey1();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", appId);
        params.put("app_trans_id", appTransId.trim());
        params.put("mac", hmacSha256(properties.getKey1(), data));

        Map<String, Object> responseBody = postForm(properties.getQueryOrderUrl(), params, "ZaloPay truy vấn giao dịch");
        return new QueryStatusResponse(
                toInt(firstValue(responseBody, "return_code", "returncode"), -1),
                firstNotBlank(value(responseBody, "return_message"), value(responseBody, "returnmessage")),
                toBoolean(firstValue(responseBody, "is_processing", "isprocessing")),
                parseLong(firstNotBlank(value(responseBody, "amount")), -1),
                parseLong(firstNotBlank(value(responseBody, "discount_amount"), value(responseBody, "discountamount")), 0),
                firstNotBlank(value(responseBody, "zp_trans_id"), value(responseBody, "zptransid")),
                responseBody
        );
    }

    @Override
    public BankListResponse getSupportedBanks() {
        ensureBankListConfigured();
        String appId = firstNotBlank(properties.getBankListAppId(), properties.getAppId());
        String key1 = firstNotBlank(properties.getBankListKey1(), properties.getKey1());
        String reqTime = String.valueOf(System.currentTimeMillis());

        Map<String, String> params = new LinkedHashMap<>();
        params.put("appid", appId);
        params.put("reqtime", reqTime);
        params.put("mac", hmacSha256(key1, appId + "|" + reqTime));

        Map<String, Object> responseBody = postForm(properties.getBankListUrl(), params, "ZaloPay lấy danh sách ngân hàng");
        Object banksValue = responseBody.get("banks");
        Map<String, Object> banks = banksValue instanceof Map<?, ?> raw ? normalizeMap(raw) : Map.of();
        return new BankListResponse(
                toInt(firstValue(responseBody, "returncode", "return_code"), -1),
                firstNotBlank(value(responseBody, "returnmessage"), value(responseBody, "return_message")),
                banks,
                responseBody
        );
    }

    @Override
    public String value(Map<String, ?> payload, String key) {
        if (payload == null || key == null) {
            return "";
        }
        Object value = payload.get(key);
        return value != null ? String.valueOf(value) : "";
    }

    private Map<String, Object> postForm(String url, Map<String, String> params, String action) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(35))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(formEncode(params), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(action + " trả HTTP " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(action + " thất bại: " + e.getMessage(), e);
        }
    }

    private String formEncode(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append('&');
            }
            builder.append(encode(entry.getKey()))
                    .append('=')
                    .append(encode(entry.getValue()));
        }
        return builder.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    private Object firstValue(Map<String, ?> payload, String... keys) {
        if (payload == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Map<String, Object> normalizeMap(Map<?, ?> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private void ensureConfigured() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Thanh toán ZaloPay đang tắt cấu hình");
        }
        if (isBlank(properties.getAppId()) || isBlank(properties.getKey1()) || isBlank(properties.getKey2())) {
            throw new IllegalStateException("Chưa cấu hình zalopay.app-id, zalopay.key1 và zalopay.key2");
        }
        if (isBlank(properties.getCreateOrderUrl()) || isBlank(properties.getQueryOrderUrl())) {
            throw new IllegalStateException("Chưa cấu hình endpoint ZaloPay");
        }
    }

    private void ensureBankListConfigured() {
        ensureConfigured();
        if (isBlank(properties.getBankListUrl())) {
            throw new IllegalStateException("Chưa cấu hình zalopay.bank-list-url");
        }
    }

    private String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo chữ ký ZaloPay", e);
        }
    }

    private boolean constantEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                firstNotBlank(expected).toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                firstNotBlank(actual).toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
        );
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

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
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
}
