package vn.hoadon.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.hoadon.services.TaxCodeLookupService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class TaxCodeLookupServiceImpl implements TaxCodeLookupService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String appId;
    private final String appSecret;
    private final String cassoBaseUrl;
    private final String legacyBaseUrl;

    private volatile String accessToken;
    private volatile Instant accessTokenExpiresAt = Instant.EPOCH;

    public TaxCodeLookupServiceImpl(
            ObjectMapper objectMapper,
            @Value("${tax-lookup.casso.app-id:}") String appId,
            @Value("${tax-lookup.casso.app-secret:}") String appSecret,
            @Value("${tax-lookup.casso.base-url:https://openapi.casso.vn}") String cassoBaseUrl,
            @Value("${tax-lookup.legacy-base-url:https://api.vietqr.io/v2/business}") String legacyBaseUrl
    ) {
        this.objectMapper = objectMapper;
        this.appId = trim(appId);
        this.appSecret = trim(appSecret);
        this.cassoBaseUrl = stripTrailingSlash(cassoBaseUrl);
        this.legacyBaseUrl = stripTrailingSlash(legacyBaseUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(7))
                .build();
    }

    @Override
    public Taxpayer lookup(String rawTaxCode) {
        String taxCode = normalizeTaxCode(rawTaxCode);
        if (!taxCode.matches("(?:\\d{10}(?:-\\d{3})?|\\d{12})")) {
            throw new LookupException(400, "Mã số thuế phải gồm 10 số, 10 số-3 số chi nhánh hoặc 12 số");
        }

        if (isCassoConfigured()) {
            return lookupCasso(taxCode, true);
        }
        if (taxCode.matches("\\d{12}")) {
            throw new LookupException(503, "Tra cứu hộ kinh doanh cần cấu hình CASSO_APP_ID và CASSO_APP_SECRET");
        }
        return lookupLegacy(taxCode);
    }

    private Taxpayer lookupCasso(String taxCode, boolean allowTokenRefresh) {
        try {
            String resource = taxCode.matches("\\d{12}") ? "household" : "business";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cassoBaseUrl + "/idkit/v1/" + resource + "/" + taxCode))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + getAccessToken())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401 && allowTokenRefresh) {
                clearAccessToken();
                return lookupCasso(taxCode, false);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerError(response.statusCode(), response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode taxpayer = firstObject(root.path(resource), root.path("business"), root.path("household"), root.path("data"));
            String name = text(taxpayer, "name");
            if (name.isBlank()) {
                throw new LookupException(404, "Không tìm thấy thông tin cho mã số thuế " + taxCode);
            }
            return new Taxpayer(
                    firstNotBlank(text(taxpayer, "taxCode"), taxCode),
                    name,
                    text(taxpayer, "address"),
                    text(taxpayer, "status"),
                    text(taxpayer, "taxDepartment"),
                    firstNotBlank(text(taxpayer, "source"), "Casso IDKit")
            );
        } catch (LookupException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new LookupException(502, "Tra cứu mã số thuế bị gián đoạn", ex);
        } catch (Exception ex) {
            throw new LookupException(502, "Không thể kết nối dịch vụ tra cứu mã số thuế", ex);
        }
    }

    private Taxpayer lookupLegacy(String taxCode) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(legacyBaseUrl + "/" + taxCode))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerError(response.statusCode(), response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (!"00".equals(root.path("code").asText())) {
                String message = firstNotBlank(root.path("desc").asText(), "Không tìm thấy mã số thuế " + taxCode);
                throw new LookupException("51".equals(root.path("code").asText()) ? 400 : 404, message);
            }
            JsonNode data = root.path("data");
            return new Taxpayer(
                    firstNotBlank(text(data, "id"), taxCode),
                    text(data, "name"),
                    text(data, "address"),
                    text(data, "status"),
                    "",
                    "VietQR"
            );
        } catch (LookupException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new LookupException(502, "Tra cứu mã số thuế bị gián đoạn", ex);
        } catch (Exception ex) {
            throw new LookupException(502, "Không thể kết nối dịch vụ tra cứu mã số thuế", ex);
        }
    }

    private synchronized String getAccessToken() throws Exception {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpiresAt)) {
            return accessToken;
        }

        String body = objectMapper.writeValueAsString(Map.of("app_id", appId, "app_secret", appSecret));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cassoBaseUrl + "/oauth/token"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw providerError(response.statusCode(), response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        String token = json.path("access_token").asText("").trim();
        if (token.isBlank()) {
            throw new LookupException(502, "Dịch vụ tra cứu không trả về access token");
        }
        long expiresIn = Math.max(120L, json.path("expires_in").asLong(900L));
        accessToken = token;
        accessTokenExpiresAt = Instant.now().plusSeconds(Math.max(60L, expiresIn - 60L));
        return accessToken;
    }

    private synchronized void clearAccessToken() {
        accessToken = null;
        accessTokenExpiresAt = Instant.EPOCH;
    }

    private LookupException providerError(int providerStatus, String responseBody) {
        int clientStatus = switch (providerStatus) {
            case 400 -> 400;
            case 404 -> 404;
            case 429 -> 429;
            default -> 502;
        };
        return new LookupException(clientStatus, providerMessage(responseBody));
    }

    private String providerMessage(String responseBody) {
        String defaultMessage = "Dịch vụ tra cứu mã số thuế tạm thời không phản hồi";
        try {
            JsonNode json = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
            return firstNotBlank(
                    json.path("message").asText(),
                    json.path("error_description").asText(),
                    json.path("error").path("message").asText(),
                    json.path("desc").asText(),
                    defaultMessage
            );
        } catch (Exception ignored) {
            return defaultMessage;
        }
    }

    private boolean isCassoConfigured() {
        return !appId.isBlank() && !appSecret.isBlank();
    }

    private String normalizeTaxCode(String value) {
        String taxCode = trim(value).replaceAll("\\s+", "");
        if (taxCode.matches("\\d{13}")) {
            return taxCode.substring(0, 10) + "-" + taxCode.substring(10);
        }
        return taxCode;
    }

    private static JsonNode firstObject(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && node.isObject() && !node.isEmpty()) return node;
        }
        return nodes.length > 0 ? nodes[0] : null;
    }

    private static String text(JsonNode node, String field) {
        return node == null ? "" : trim(node.path(field).asText(""));
    }

    private static String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private static String stripTrailingSlash(String value) {
        return trim(value).replaceAll("/+$", "");
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
