package org.example.sharedprompts.auth.rate.filter.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.stereotype.Service;

/**
 * Rate Limit 메트릭 서비스
 *
 * Rate Limit 관련 메트릭을 Micrometer를 통해 수집합니다.
 *
 * ⚠️ 주의:
 * - Meter는 (name + tags) 조합 기준으로 캐시되므로
 *   매 요청마다 Counter.builder().register()를 호출하지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class RateLimitMetricsService {

    private static final String RATE_LIMIT_CHECK = "rate.limit.check";
    private static final String RATE_LIMIT_EXCEEDED = "rate.limit.exceeded";
    private static final String RATE_LIMIT_CHECK_FAILED = "rate.limit.check.failed";
    private static final String RATE_LIMIT_FAIL_OPEN = "rate.limit.fail.open";

    private final MeterRegistry meterRegistry;

    /**
     * Rate Limit 체크 메트릭 기록
     */
    public void recordRateLimitCheck(
            RateLimitRule rule,
            RateLimiter.RateLimitResult result,
            boolean isExceeded
    ) {
        meterRegistry.counter(
                RATE_LIMIT_CHECK,
                "rule", safeRuleName(rule),
                "exceeded", String.valueOf(isExceeded)
        ).increment();
    }

    /**
     * Rate Limit 초과 메트릭 기록
     */
    public void recordRateLimitExceeded(
            RateLimitRule rule,
            long currentCount,
            long limit
    ) {
        meterRegistry.counter(
                RATE_LIMIT_EXCEEDED,
                "rule", safeRuleName(rule)
        ).increment();
    }

    /**
     * Rate Limit 체크 실패 메트릭 기록
     */
    public void recordRateLimitCheckFailed(RateLimitRule rule) {
        meterRegistry.counter(
                RATE_LIMIT_CHECK_FAILED,
                "rule", safeRuleName(rule)
        ).increment();
    }

    /**
     * Rate Limit Fail-Open 메트릭 기록
     */
    public void recordRateLimitFailOpen(RateLimitRule rule) {
        meterRegistry.counter(
                RATE_LIMIT_FAIL_OPEN,
                "rule", safeRuleName(rule)
        ).increment();
    }

    /**
     * rule 이름 null-safe 처리
     */
    private String safeRuleName(RateLimitRule rule) {
        return rule != null ? rule.getName() : "unknown";
    }
}
