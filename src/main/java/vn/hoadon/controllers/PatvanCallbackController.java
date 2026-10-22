package vn.hoadon.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.services.patvan.PatvanCallbackService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/api/patvan")
public class PatvanCallbackController {
    private static final Logger log = LoggerFactory.getLogger(PatvanCallbackController.class);

    private final PatvanCallbackService callbackService;
    private final String expectedProviderTaxCode;

    public PatvanCallbackController(
            PatvanCallbackService callbackService,
            @Value("${patvan.callback.provider-tax-code:0302431595}") String expectedProviderTaxCode) {
        this.callbackService = callbackService;
        this.expectedProviderTaxCode = expectedProviderTaxCode;
    }

    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(@RequestBody CallbackRequest request,
                                                        HttpServletRequest httpRequest) {
        Map<String, Object> response = new LinkedHashMap<>();
        String requestId = UUID.randomUUID().toString();
        long startedAt = System.currentTimeMillis();
        response.put("requestId", requestId);
        log.info("PATVAN_CALLBACK_RECEIVED requestId={} remoteIp={} providerMst={} taxpayerMst={} xmlLength={}",
                requestId,
                remoteIp(httpRequest),
                request != null ? trim(request.mst()) : "",
                request != null ? trim(request.mstNnt()) : "",
                request != null && request.xml() != null ? request.xml().length() : 0);
        try {
            if (request == null) throw new PatvanCallbackService.CallbackException("Dữ liệu callback rỗng");
            if (expectedProviderTaxCode != null && !expectedProviderTaxCode.isBlank()
                    && !expectedProviderTaxCode.trim().equals(trim(request.mst()))) {
                throw new PatvanCallbackService.CallbackException("mst của đơn vị truyền nhận không hợp lệ");
            }
            PatvanCallbackService.Result result = callbackService.process(
                    request.mst(), request.mstNnt(), request.xml());
            response.put("status", result.status());
            response.put("message", result.message());
            response.put("messageType", result.messageType());
            response.put("messageReference", result.messageReference());
            log.info("PATVAN_CALLBACK_PROCESSED requestId={} status={} messageType={} reference={} companyId={} durationMs={}",
                    requestId, result.status(), result.messageType(), result.messageReference(),
                    result.companyId(), System.currentTimeMillis() - startedAt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("PATVAN_CALLBACK_FAILED requestId={} providerMst={} taxpayerMst={} xmlLength={} durationMs={} error={}",
                    requestId,
                    request != null ? trim(request.mst()) : "",
                    request != null ? trim(request.mstNnt()) : "",
                    request != null && request.xml() != null ? request.xml().length() : 0,
                    System.currentTimeMillis() - startedAt,
                    e.getMessage(), e);
            // CallbackPatvanController.php đọc trường status trong body để quyết định retry.
            response.put("status", 404);
            response.put("message", e.getMessage() == null ? "Xử lý callback thất bại" : e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String remoteIp(HttpServletRequest request) {
        if (request == null) return "";
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    public record CallbackRequest(String mst, String mstNnt, String xml) {}
}
