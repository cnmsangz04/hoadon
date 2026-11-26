package vn.hoadon.ai.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.hoadon.ai.persistence.AiUsageAggregate;
import vn.hoadon.ai.persistence.AiUsageLogEntity;
import vn.hoadon.ai.persistence.AiUsageLogRepository;

import java.time.LocalDateTime;

@Service
public class AiUsageQueryService {
    private final AiUsageLogRepository repository;

    public AiUsageQueryService(AiUsageLogRepository repository) {
        this.repository = repository;
    }

    public Page<AiUsageLogEntity> find(Long companyId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        validateRange(from, to);
        return repository.findUsage(companyId, from, to, pageable);
    }

    public AiUsageSummary summarize(Long companyId, LocalDateTime from, LocalDateTime to) {
        validateRange(from, to);
        AiUsageAggregate aggregate = repository.aggregateUsage(companyId, from, to);
        return new AiUsageSummary(value(aggregate.getRequestCount()), value(aggregate.getSuccessCount()),
                value(aggregate.getFailureCount()), value(aggregate.getInputTokens()),
                value(aggregate.getOutputTokens()), value(aggregate.getTotalTokens()));
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("Khoảng thời gian thống kê AI không hợp lệ");
        }
        if (from.plusDays(366).isBefore(to)) {
            throw new IllegalArgumentException("Chỉ được thống kê AI tối đa 366 ngày");
        }
    }

    private long value(Long number) {
        return number == null ? 0 : number;
    }

    public record AiUsageSummary(long requestCount, long successCount, long failureCount,
                                 long inputTokens, long outputTokens, long totalTokens) {
    }
}
