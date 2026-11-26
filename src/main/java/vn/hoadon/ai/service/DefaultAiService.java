package vn.hoadon.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.hoadon.ai.config.AiProperties;
import vn.hoadon.ai.model.AiExecutionContext;
import vn.hoadon.ai.model.AiRequest;
import vn.hoadon.ai.model.AiResponse;
import vn.hoadon.ai.provider.AiProvider;
import vn.hoadon.ai.provider.AiProviderException;
import vn.hoadon.ai.provider.AiProviderRequest;
import vn.hoadon.ai.provider.AiProviderResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class DefaultAiService implements AiService {
    private static final Logger log = LoggerFactory.getLogger(DefaultAiService.class);

    private final AiProperties properties;
    private final Map<String, AiProvider> providers;
    private final AiDataSanitizer dataSanitizer;
    private final AiScopeValidator scopeValidator;
    private final AiRateLimiter rateLimiter;
    private final AiUsageRecorder usageRecorder;

    public DefaultAiService(AiProperties properties, List<AiProvider> providers,
                            AiDataSanitizer dataSanitizer, AiScopeValidator scopeValidator,
                            AiRateLimiter rateLimiter,
                            AiUsageRecorder usageRecorder) {
        this.properties = properties;
        this.providers = indexProviders(providers);
        this.dataSanitizer = dataSanitizer;
        this.scopeValidator = scopeValidator;
        this.rateLimiter = rateLimiter;
        this.usageRecorder = usageRecorder;
    }

    @Override
    public AiResponse generate(AiExecutionContext context, AiRequest request) {
        validate(context, request);
        AiProvider provider = selectedProvider();
        String requestId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        int inputCharacters = totalCharacters(request);
        int attempts = 0;

        AiProviderRequest providerRequest = toProviderRequest(request);
        try {
            while (true) {
                attempts++;
                rateLimiter.acquire(context.companyId(), estimatedInputTokens(providerRequest));
                try {
                    AiProviderResponse response = provider.generate(providerRequest);
                    long durationMs = elapsedMillis(startedAt);
                    usageRecorder.success(requestId, context, provider.name(), response,
                            inputCharacters, durationMs, attempts);
                    return new AiResponse(requestId, response.text(), response.structuredOutput(),
                            response.functionCalls(), response.usage(), provider.name(), response.model(),
                            response.finishReason());
                } catch (AiProviderException ex) {
                    if (!shouldRetry(ex, attempts)) throw ex;
                    waitBeforeRetry(ex, attempts);
                }
            }
        } catch (AiProviderException ex) {
            usageRecorder.failure(requestId, context, provider.name(), configuredModel(provider), ex.getCode(),
                    inputCharacters, elapsedMillis(startedAt), attempts);
            log.warn("AI request {} failed: provider={}, code={}, attempts={}",
                    requestId, provider.name(), ex.getCode(), attempts);
            throw new AiServiceException(ex.getCode(), publicErrorMessage(ex), ex);
        } catch (AiServiceException ex) {
            usageRecorder.failure(requestId, context, provider.name(), configuredModel(provider), ex.getCode(),
                    inputCharacters, elapsedMillis(startedAt), attempts);
            throw ex;
        } catch (RuntimeException ex) {
            usageRecorder.failure(requestId, context, provider.name(), configuredModel(provider), "AI_INTERNAL_ERROR",
                    inputCharacters, elapsedMillis(startedAt), attempts);
            log.error("AI request {} failed unexpectedly", requestId, ex);
            throw new AiServiceException("AI_INTERNAL_ERROR", "Không thể xử lý yêu cầu AI", ex);
        }
    }

    private void validate(AiExecutionContext context, AiRequest request) {
        if (context == null || request == null) {
            throw new IllegalArgumentException("Yêu cầu AI không hợp lệ");
        }
        scopeValidator.validate(context);
        if (!properties.isEnabled()) {
            throw new AiServiceException("AI_DISABLED", "Chức năng AI đang tắt");
        }
        if (!properties.isFeatureEnabled(context.feature())) {
            throw new AiServiceException("AI_FEATURE_DISABLED", "Chức năng AI này đang tắt");
        }
        if (totalCharacters(request) > Math.max(1, properties.getMaxInputChars())) {
            throw new AiServiceException("AI_INPUT_TOO_LARGE", "Nội dung vượt quá giới hạn xử lý AI");
        }
    }

    private AiProvider selectedProvider() {
        String configured = normalize(properties.getProvider());
        AiProvider provider = providers.get(configured);
        if (provider == null) {
            throw new AiServiceException("AI_PROVIDER_UNKNOWN", "Nhà cung cấp AI chưa được hỗ trợ");
        }
        if (!provider.isConfigured()) {
            throw new AiServiceException("AI_NOT_CONFIGURED", "AI chưa được cấu hình API Key");
        }
        return provider;
    }

    private AiProviderRequest toProviderRequest(AiRequest request) {
        int configuredMax = Math.max(1, properties.getDefaultMaxOutputTokens());
        int requestedMax = request.maxOutputTokens() == null ? configuredMax : request.maxOutputTokens();
        int maxOutputTokens = Math.min(requestedMax, configuredMax);
        return new AiProviderRequest(
                dataSanitizer.sanitize(request.systemInstruction()),
                dataSanitizer.sanitize(request.prompt()),
                request.responseJsonSchema(),
                request.functions(),
                maxOutputTokens,
                request.temperature()
        );
    }

    private boolean shouldRetry(AiProviderException ex, int attempts) {
        return ex.isRetryable() && attempts < Math.max(1, properties.getRetry().getMaxAttempts());
    }

    private void waitBeforeRetry(AiProviderException ex, int attempts) {
        long initial = Math.max(0, properties.getRetry().getInitialDelayMs());
        long configuredMax = Math.max(initial, properties.getRetry().getMaxDelayMs());
        long exponential = initial;
        for (int i = 1; i < attempts && exponential < configuredMax; i++) {
            exponential = Math.min(configuredMax, exponential * 2);
        }
        long delay = Math.min(configuredMax, Math.max(exponential, ex.getRetryAfterMs()));
        if (delay <= 0) return;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("AI_INTERRUPTED", "Yêu cầu AI đã bị gián đoạn", interrupted);
        }
    }

    private int totalCharacters(AiRequest request) {
        int systemLength = request.systemInstruction() == null ? 0 : request.systemInstruction().length();
        return systemLength + request.prompt().length();
    }

    private int estimatedInputTokens(AiProviderRequest request) {
        int characters = request.prompt().length()
                + (request.systemInstruction() == null ? 0 : request.systemInstruction().length());
        return Math.max(1, (characters + 3) / 4);
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private String configuredModel(AiProvider provider) {
        return "gemini".equals(provider.name()) ? properties.getGemini().getModel() : provider.name();
    }

    private String publicErrorMessage(AiProviderException ex) {
        return switch (ex.getCode()) {
            case "AI_QUOTA_EXHAUSTED" -> "Gemini đã hết hạn mức, vui lòng thử lại sau";
            case "AI_UNAUTHORIZED" -> "API Key Gemini không hợp lệ";
            case "AI_INVALID_RESPONSE" -> "Gemini trả về dữ liệu không hợp lệ";
            case "AI_CONTENT_BLOCKED" -> "Gemini từ chối nội dung yêu cầu";
            case "AI_TIMEOUT" -> "Gemini phản hồi quá thời gian cho phép";
            default -> "Dịch vụ AI tạm thời không khả dụng";
        };
    }

    private Map<String, AiProvider> indexProviders(List<AiProvider> providerList) {
        Map<String, AiProvider> result = new LinkedHashMap<>();
        for (AiProvider provider : providerList) {
            String name = normalize(provider.name());
            if (result.putIfAbsent(name, provider) != null) {
                throw new IllegalStateException("Trùng tên AI provider: " + name);
            }
        }
        return Map.copyOf(result);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
