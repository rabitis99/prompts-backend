package org.example.sharedprompts.auth.rate.filter.metrics;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rate Limit 메트릭 수집기
 * 
 * Rate Limit 관련 메트릭을 수집하는 컴포넌트입니다.
 * Observer 패턴을 통해 Rate Limit 체크와 메트릭 수집을 분리합니다.
 * 여러 관찰자를 등록하여 다양한 메트릭 수집 전략을 적용할 수 있습니다.
 */
@Component
@RequiredArgsConstructor
public class RateLimitMetricsCollector implements RateLimitMetricsObserver {

    private final List<RateLimitMetricsObserver> observers;

    /**
     * Rate Limit 체크 메트릭을 기록합니다.
     * 
     * @param rule RateLimitRule
     * @param result RateLimitResult
     */
    public void recordCheck(RateLimitRule rule, RateLimiter.RateLimitResult result) {
        onCheck(rule, result);
    }

    /**
     * Rate Limit 초과 메트릭을 기록합니다.
     * 
     * @param rule RateLimitRule
     * @param currentCount 현재 카운트
     * @param limit 제한 값
     */
    public void recordExceeded(RateLimitRule rule, long currentCount, long limit) {
        onExceeded(rule, currentCount, limit);
    }

    /**
     * Rate Limit 체크 실패 메트릭을 기록합니다.
     * 
     * @param rule RateLimitRule
     */
    public void recordFailed(RateLimitRule rule) {
        onFailed(rule);
    }

    @Override
    public void onCheck(RateLimitRule rule, RateLimiter.RateLimitResult result) {
        observers.forEach(observer -> observer.onCheck(rule, result));
    }

    @Override
    public void onExceeded(RateLimitRule rule, long currentCount, long limit) {
        observers.forEach(observer -> observer.onExceeded(rule, currentCount, limit));
    }

    @Override
    public void onFailed(RateLimitRule rule) {
        observers.forEach(observer -> observer.onFailed(rule));
    }
}

