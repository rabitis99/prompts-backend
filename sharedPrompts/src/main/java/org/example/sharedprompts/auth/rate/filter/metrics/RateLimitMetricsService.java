package org.example.sharedprompts.auth.rate.filter.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.stereotype.Service;

/**
 * Rate Limit 메트릭 서비스
 * 
 * Rate Limit 관련 메트릭을 Micrometer를 통해 수집합니다.
 */
@Service
@RequiredArgsConstructor
public class RateLimitMetricsService {

    private static final String RATE_LIMIT_CHECK = "rate.limit.check";
    private static final String RATE_LIMIT_EXCEEDED = "rate.limit.exceeded";
    private static final String RATE_LIMIT_CHECK_FAILED = "rate.limit.check.failed";

    private final MeterRegistry meterRegistry;

    /**
     * Rate Limit 체크 메트릭을 기록합니다.
     * 
     * @param rule RateLimitRule
     * @param result RateLimitResult
     * @param isExceeded 초과 여부
     */
    public void recordRateLimitCheck(RateLimitRule rule, RateLimiter.RateLimitResult result, boolean isExceeded) {
        Counter.builder(RATE_LIMIT_CHECK)
                .tag("rule", rule.getName())
                .tag("exceeded", String.valueOf(isExceeded))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Rate Limit 초과 메트릭을 기록합니다.
     * 
     * @param rule RateLimitRule
     * @param currentCount 현재 카운트
     * @param limit 제한 값
     */
    public void recordRateLimitExceeded(RateLimitRule rule, long currentCount, long limit) {
        Counter.builder(RATE_LIMIT_EXCEEDED)
                .tag("rule", rule.getName())
                .register(meterRegistry)
                .increment();
    }

    /**
     * Rate Limit 체크 실패 메트릭을 기록합니다.
     * 
     * @param rule RateLimitRule
     */
    public void recordRateLimitCheckFailed(RateLimitRule rule) {
        Counter.builder(RATE_LIMIT_CHECK_FAILED)
                .tag("rule", rule.getName())
                .register(meterRegistry)
                .increment();
    }
}
