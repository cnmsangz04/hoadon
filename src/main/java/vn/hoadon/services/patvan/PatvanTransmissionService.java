package vn.hoadon.services.patvan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PatvanTransmissionService {
    private static final Logger log = LoggerFactory.getLogger(PatvanTransmissionService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpClient httpClient;

    @Value("${patvan.uat.ssl-verify:false}")
    private boolean sslVerify;

    @Value("${patvan.uat.server-cert-sha256:8C2EBE5EA8DBCC68510D9E9A92FD6AE699F33563149785E1B6BBB9CFE0E2E6DA}")
    private String pinnedServerCertificateSha256;

    @Value("${patvan.uat.oauth-url:https://ote-tvan.hoadon30s.vn/token}")
    private String oauthUrl;

    @Value("${patvan.uat.oauth-mtt-url:https://ote-tvan-mtt.hoadon30s.vn/token}")
    private String oauthMttUrl;

    @Value("${patvan.uat.oauth-ct-url:https://ote-tvan-chungtu.hoadon30s.vn/token}")
    private String oauthCtUrl;

    @Value("${patvan.uat.client-id:}")
    private String clientId;

    @Value("${patvan.uat.client-secret:}")
    private String clientSecret;

    @Value("${patvan.uat.client-mtt-id:}")
    private String clientMttId;

    @Value("${patvan.uat.client-mtt-secret:}")
    private String clientMttSecret;

    @Value("${patvan.uat.client-ct-id:}")
    private String clientCtId;

    @Value("${patvan.uat.client-ct-secret:}")
    private String clientCtSecret;

    @Value("${patvan.uat.hd-url:https://ote-tvan.hoadon30s.vn/hoa-don-co-ma}")
    private String hdUrl;

    @Value("${patvan.uat.hdkm-url:https://ote-tvan.hoadon30s.vn/hoa-don-khong-ma}")
    private String hdkmUrl;

    @Value("${patvan.uat.hdmtt-url:https://ote-tvan-mtt.hoadon30s.vn/hoa-don-may-tinh-tien}")
    private String hdmttUrl;

    @Value("${patvan.uat.cttncn-url:https://ote-tvan-chungtu.hoadon30s.vn/chung-tu-khau-tru-thue}")
    private String cttncnUrl;

    @Value("${patvan.uat.dksd-url:https://ote-tvan.hoadon30s.vn/dang-ky-to-khai}")
    private String dksdUrl;

    @Value("${patvan.uat.dksdctdt-url:https://ote-tvan-chungtu.hoadon30s.vn/to-khai-chung-tu-dien-tu}")
    private String dksdctdtUrl;

    @Value("${patvan.uat.tbss-url:https://ote-tvan.hoadon30s.vn/thong-bao-sai-sot}")
    private String tbssUrl;

    @Value("${patvan.uat.tbss-mtt-url:https://ote-tvan-mtt.hoadon30s.vn/thong-bao-sai-sot-may-tinh-tien}")
    private String tbssMttUrl;

    @Value("${patvan.uat.tbss-ct-url:https://ote-tvan-chungtu.hoadon30s.vn/chung-tu-sai-sot}")
    private String tbssCtUrl;

    @Value("${patvan.uat.bthhd-url:https://ote-tvan.hoadon30s.vn/bang-tong-hop}")
    private String bthhdUrl;

    @PostConstruct
    private void initializeHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20));
        if (!sslVerify) {
            try {
                builder.sslContext(pinnedUatSslContext(pinnedServerCertificateSha256));
                log.warn("PATVAN UAT CA-chain verification is disabled; the UAT leaf certificate remains pinned");
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Không khởi tạo được SSL client cho PATVAN UAT", e);
            }
        }
        httpClient = builder.build();
    }

    public PatvanPostResult postXml(PatvanMessageType type, String xml, String mstNnt) {
        if (type == null) {
            throw new PatvanException("Loại thông điệp truyền nhận không hợp lệ");
        }
        if (xml == null || xml.isBlank()) {
            throw new PatvanException("XML gửi truyền nhận đang trống");
        }
        if (mstNnt == null || mstNnt.isBlank()) {
            throw new PatvanException("Thiếu mã số thuế người nộp thuế");
        }

        Token token = getToken(type);
        String url = endpoint(type);
        String xmlData = Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));

        Map<String, String> form = new LinkedHashMap<>();
        form.put("xmlData", xmlData);
        form.put("mstNnt", mstNnt.trim());

        try {
            log.info("REQUEST_SEND_PATVAN_UAT type={} url={} mstNnt={} xmlBytes={}", type, url, mstNnt, xml.length());
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", token.tokenType() + " " + token.accessToken())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody(form), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body() != null ? response.body() : "";
            log.info("RESPONSE_SEND_PATVAN_UAT type={} httpStatus={} body={}", type, response.statusCode(), body.replace("\n", ""));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PatvanException("Truyền nhận UAT trả HTTP " + response.statusCode());
            }
            JsonNode root = JSON.readTree(body);
            int status = root.path("status").asInt(0);
            String message = root.path("message").asText("");
            if (status != 200) {
                throw new PatvanException(message.isBlank() ? "Truyền nhận UAT từ chối thông điệp" : message);
            }
            String maThongdiep = root.path("data").path("maThongdiep").asText("");
            if (maThongdiep.isBlank()) {
                throw new PatvanException("Truyền nhận UAT không trả mã thông điệp");
            }
            return new PatvanPostResult(status, message, maThongdiep, body);
        } catch (PatvanException e) {
            throw e;
        } catch (Exception e) {
            throw new PatvanException("Không gửi được dữ liệu lên truyền nhận UAT: " + e.getMessage(), e);
        }
    }

    private Token getToken(PatvanMessageType type) {
        Credential credential = credential(type);
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "client_credentials");
        form.put("client_id", credential.clientId());
        form.put("client_secret", credential.clientSecret());

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(credential.oauthUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody(form), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body() != null ? response.body() : "";
            log.info("RESPONSE_GET_PATVAN_TOKEN_UAT group={} httpStatus={}", credential.group(), response.statusCode());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PatvanException("Không lấy được token truyền nhận UAT, HTTP " + response.statusCode());
            }
            JsonNode root = JSON.readTree(body);
            int status = root.path("status").asInt(0);
            String message = root.path("message").asText("");
            if (status != 200) {
                throw new PatvanException(message.isBlank() ? "Không lấy được token truyền nhận UAT" : message);
            }
            String tokenType = root.path("token_type").asText("Bearer");
            String accessToken = root.path("access_token").asText("");
            if (accessToken.isBlank()) {
                throw new PatvanException("Token truyền nhận UAT rỗng");
            }
            return new Token(tokenType, accessToken);
        } catch (PatvanException e) {
            throw e;
        } catch (Exception e) {
            throw new PatvanException("Không lấy được token truyền nhận UAT: " + e.getMessage(), e);
        }
    }

    private Credential credential(PatvanMessageType type) {
        if (isChungTu(type)) {
            return new Credential("CT", oauthCtUrl, clientCtId, clientCtSecret);
        }
        if (isMayTinhTien(type)) {
            return new Credential("MTT", oauthMttUrl, clientMttId, clientMttSecret);
        }
        return new Credential("DEFAULT", oauthUrl, clientId, clientSecret);
    }

    private String endpoint(PatvanMessageType type) {
        return switch (type) {
            case HD -> hdUrl;
            case HDKM -> hdkmUrl;
            case HDMTT -> hdmttUrl;
            case CTTNCN -> cttncnUrl;
            case DKSD -> dksdUrl;
            case DKSDCTDT -> dksdctdtUrl;
            case TBSS -> tbssUrl;
            case TBSS_MTT -> tbssMttUrl;
            case TBSS_CT -> tbssCtUrl;
            case BTHHD -> bthhdUrl;
        };
    }

    private boolean isChungTu(PatvanMessageType type) {
        return type == PatvanMessageType.DKSDCTDT
                || type == PatvanMessageType.CTTNCN
                || type == PatvanMessageType.TBSS_CT;
    }

    private boolean isMayTinhTien(PatvanMessageType type) {
        return type == PatvanMessageType.HDMTT || type == PatvanMessageType.TBSS_MTT;
    }

    private String formBody(Map<String, String> form) {
        return form.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    private SSLContext pinnedUatSslContext(String expectedFingerprint) throws GeneralSecurityException {
        String expected = normalizeFingerprint(expectedFingerprint);
        if (expected.isBlank()) {
            throw new GeneralSecurityException("Thiếu fingerprint certificate PATVAN UAT");
        }
        TrustManager[] trustManagers = {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                        if (chain == null || chain.length == 0) {
                            throw new CertificateException("PATVAN UAT không gửi certificate");
                        }
                        try {
                            String actual = hex(MessageDigest.getInstance("SHA-256")
                                    .digest(chain[0].getEncoded()));
                            if (!expected.equals(actual)) {
                                throw new CertificateException("Certificate PATVAN UAT không khớp fingerprint đã cấu hình");
                            }
                        } catch (GeneralSecurityException e) {
                            throw new CertificateException("Không kiểm tra được certificate PATVAN UAT", e);
                        }
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustManagers, new SecureRandom());
        return context;
    }

    private static String normalizeFingerprint(String value) {
        return value == null ? "" : value.replace(":", "").replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) result.append(String.format(Locale.ROOT, "%02X", b));
        return result.toString();
    }

    private record Token(String tokenType, String accessToken) {}
    private record Credential(String group, String oauthUrl, String clientId, String clientSecret) {}

    public static class PatvanException extends RuntimeException {
        public PatvanException(String message) {
            super(message);
        }

        public PatvanException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
