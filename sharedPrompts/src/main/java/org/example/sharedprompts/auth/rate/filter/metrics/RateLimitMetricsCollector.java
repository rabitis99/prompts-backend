package org.example.sharedprompts.auth.rate.filter.metrics;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.stereotype.Component;

/**
 * Rate Limit 메트릭 수집기
 * 
 * Rate Limit 관련 메트릭을 수집하는 컴포넌트입니다.
 * Observer 패턴을 통해 Rate Limit 체크와 메트릭 수집을 분리합니다.
 */
@Component
@RequiredArgsConstructor
public class RateLimitMetricsCollector {

    private final RateLimitMetricsService metricsService;

    /**
     * Rate Limit 체크 메트릭을 기록합니다.
     * 
     * @param rule RateLimitRule
     * @param result RateLimitResult
     */
    public void recordCheck(RateLimitRule rule, RateLimiter.RateLimitResult result) {
        if (result == null) {
            metricsService.recordRateLimitCheckFailed(rule);
            return;
        }
        metricsService.recordRateLimitCheck(rule, result, result.isExceeded());
    }

    /**
     * Rate Limit 초과 메트릭을 기록합니다.
     * 
     * @param rule RateLimitRule
     * @param currentCount 현재 카운트
     * @param limit 제한 값
     */
    public void recordExceeded(RateLimitRule rule, long currentCount, long limit) {
        metricsService.recordRateLimitExceeded(rule, currentCount, limit);
    }

    /**
     * Rate Limit 체크 실패 메트릭을 기록합니다.
     * 
     * @param rule RateLimitRule
     */
    public void recordFailed(RateLimitRule rule) {
        metricsService.recordRateLimitCheckFailed(rule);
    }
}

