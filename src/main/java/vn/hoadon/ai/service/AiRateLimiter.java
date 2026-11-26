package vn.hoadon.ai.service;

import org.springframework.stereotype.Component;
import vn.hoadon.ai.config.AiProperties;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Component
public class AiRateLimiter {
    private static final ZoneId GEMINI_QUOTA_ZONE = ZoneId.of("America/Los_Angeles");

    private final AiProperties properties;
    private final Clock clock;
    private final Map<Long, DailyCounter> companyCounters = new HashMap<>();
    private long minuteBucket = Long.MIN_VALUE;
    private int minuteCount;
    private int minuteInputTokens;
    private LocalDate globalDate;
    private int globalDayCount;

    public AiRateLimiter(AiProperties properties, Clock aiClock) {
        this.properties = properties;
        this.clock = aiClock;
    }

    public synchronized void acquire(Long companyId) {
        acquire(companyId, 0);
    }

    public synchronized void acquire(Long companyId, int estimatedInputTokens) {
        if (companyId == null) {
            throw new AiServiceException("INVALID_CONTEXT", "Thiếu phạm vi doanh nghiệp cho yêu cầu AI");
        }

        Instant now = clock.instant();
        long currentMinute = now.getEpochSecond() / 60;
        LocalDate currentDate = now.atZone(GEMINI_QUOTA_ZONE).toLocalDate();
        resetExpiredWindows(currentMinute, currentDate);

        AiProperties.RateLimit limits = properties.getRateLimit();
        ensureAvailable(minuteCount, limits.getRequestsPerMinute(), "RATE_LIMIT_MINUTE",
                "Đã đạt giới hạn yêu cầu AI trong phút này");
        ensureAvailable(minuteInputTokens, Math.max(0, estimatedInputTokens),
                limits.getInputTokensPerMinute(), "RATE_LIMIT_TOKENS",
                "Đã đạt giới hạn token AI trong phút này");
        ensureAvailable(globalDayCount, limits.getRequestsPerDay(), "RATE_LIMIT_DAY",
                "Đã đạt hạn mức AI trong ngày");

        DailyCounter companyCounter = companyCounters.get(companyId);
        int companyCount = companyCounter == null || !currentDate.equals(companyCounter.date())
                ? 0 : companyCounter.count();
        ensureAvailable(companyCount, limits.getRequestsPerCompanyDay(), "RATE_LIMIT_COMPANY",
                "Doanh nghiệp đã đạt hạn mức AI trong ngày");

        if (companyCounter == null && companyCounters.size() >= Math.max(1, limits.getMaxTrackedCompanies())) {
            throw new AiServiceException("RATE_LIMIT_CAPACITY", "Bộ giới hạn AI đang quá tải");
        }

        minuteCount++;
        minuteInputTokens += Math.max(0, estimatedInputTokens);
        globalDayCount++;
        companyCounters.put(companyId, new DailyCounter(currentDate, companyCount + 1));
    }

    private void resetExpiredWindows(long currentMinute, LocalDate currentDate) {
        if (minuteBucket != currentMinute) {
            minuteBucket = currentMinute;
            minuteCount = 0;
            minuteInputTokens = 0;
        }
        if (!currentDate.equals(globalDate)) {
            globalDate = currentDate;
            globalDayCount = 0;
            companyCounters.entrySet().removeIf(entry -> !currentDate.equals(entry.getValue().date()));
        }
    }

    private void ensureAvailable(int current, int limit, String code, String message) {
        if (limit > 0 && current >= limit) {
            throw new AiServiceException(code, message);
        }
    }

    private void ensureAvailable(int current, int requested, int limit, String code, String message) {
        if (limit > 0 && requested > limit - current) {
            throw new AiServiceException(code, message);
        }
    }

    private record DailyCounter(LocalDate date, int count) {
    }
}
