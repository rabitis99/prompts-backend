package org.example.sharedprompts.auth.rate.filter.metrics;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.stereotype.Service;

/**
 * Rate Limit 메트릭 수집 서비스
 * 
 * Rate Limit 관련 메트릭을 수집합니다.
 * 현재는 로그 기반으로 메트릭을 기록하며, 향후 Micrometer 등을 사용하여 
 * 실제 메트릭 수집을 구현할 수 있습니다.
 */
@Slf4j
@Service
public class RateLimitMetricsService implements RateLimitMetricsObserver {

    /**
     * Rate Limit 체크 메트릭을 기록합니다.
     * 
     * @param rule RateLimitRule
     * @param result RateLimitResult
     */
    @Override
    public void onCheck(RateLimitRule rule, RateLimiter.RateLimitResult result) {
        // 향후 Micrometer 등을 사용하여 실제 메트릭 수집
        // 예: counter.increment("rate.limit.check", "rule", rule.getName(), "exceeded", String.valueOf(result.isExceeded()));
        log.debug("Rate limit check: rule={}, exceeded={}, count={}/{}", 
                rule.getName(), result.isExceeded(), result.currentCount(), result.retryAfterSeconds());
    }

    /**
     * Rate Limit 초과 메트릭을 기록합니다.
     * 
     * @param rule RateLimitRule
     * @param currentCount 현재 카운트
     * @param limit 제한 값
     */
    @Override
    public void onExceeded(RateLimitRule rule, long currentCount, long limit) {
        // 향후 Micrometer 등을 사용하여 실제 메트릭 수집
        // 예: counter.increment("rate.limit.exceeded", "rule", rule.getName());
        log.warn("Rate limit exceeded: rule={}, count={}/{}", rule.getName(), currentCount, limit);
    }

    /**
     * Rate Limit 체크 실패 메트릭을 기록합니다.
     * 
     * @param rule RateLimitRule
     */
    @Override
    public void onFailed(RateLimitRule rule) {
        // 향후 Micrometer 등을 사용하여 실제 메트릭 수집
        // 예: counter.increment("rate.limit.check.failed", "rule", rule.getName());
        log.error("Rate limit check failed: rule={}", rule.getName());
    }
}


