package vn.hoadon.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.hoadon.ai.model.AiExecutionContext;
import vn.hoadon.ai.model.AiUsage;
import vn.hoadon.ai.persistence.AiUsageLogEntity;
import vn.hoadon.ai.persistence.AiUsageLogRepository;
import vn.hoadon.ai.provider.AiProviderResponse;

@Service
public class JpaAiUsageRecorder implements AiUsageRecorder {
    private static final Logger log = LoggerFactory.getLogger(JpaAiUsageRecorder.class);

    private final AiUsageLogRepository repository;

    public JpaAiUsageRecorder(AiUsageLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void success(String requestId, AiExecutionContext context, String provider,
                        AiProviderResponse response, int inputCharacters, long durationMs, int attempts) {
        AiUsage usage = response.usage();
        save(create(requestId, context, provider, response.model(), "SUCCESS", null, inputCharacters,
                usage.inputTokens(), usage.outputTokens(), usage.totalTokens(), durationMs, attempts));
    }

    @Override
    public void failure(String requestId, AiExecutionContext context, String provider, String model,
                        String errorCode, int inputCharacters, long durationMs, int attempts) {
        save(create(requestId, context, provider, model, "FAILED", errorCode, inputCharacters,
                0, 0, 0, durationMs, attempts));
    }

    private AiUsageLogEntity create(String requestId, AiExecutionContext context, String provider, String model,
                                    String status, String errorCode, int inputCharacters, long inputTokens,
                                    long outputTokens, long totalTokens, long durationMs, int attempts) {
        AiUsageLogEntity entity = new AiUsageLogEntity();
        entity.setRequestId(requestId);
        entity.setCompanyId(context.companyId());
        entity.setUserId(context.userId());
        entity.setFeature(context.feature().getCode());
        entity.setProvider(provider);
        entity.setModel(model);
        entity.setStatus(status);
        entity.setErrorCode(errorCode);
        entity.setInputCharacters(Math.max(0, inputCharacters));
        entity.setInputTokens(Math.max(0, inputTokens));
        entity.setOutputTokens(Math.max(0, outputTokens));
        entity.setTotalTokens(Math.max(0, totalTokens));
        entity.setDurationMs(Math.max(0, durationMs));
        entity.setAttemptCount(Math.max(0, attempts));
        return entity;
    }

    private void save(AiUsageLogEntity entity) {
        try {
            repository.save(entity);
        } catch (RuntimeException ex) {
            log.warn("Cannot persist AI usage metadata for request {}", entity.getRequestId());
        }
    }
}
