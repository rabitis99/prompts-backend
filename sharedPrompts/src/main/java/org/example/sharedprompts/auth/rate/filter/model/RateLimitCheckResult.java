package org.example.sharedprompts.auth.rate.filter.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;

/**
 * Rate Limit 체크 결과
 * 
 * Rate Limit 체크 결과와 관련 정보를 담는 불변 객체입니다.
 */
@Getter
@RequiredArgsConstructor
public class RateLimitCheckResult {
    
    private final RateLimitRule rule;
    private final String key;
    private final RateLimiter.RateLimitResult result;

    /**
     * Rate Limit 체크 성공 시 결과 생성
     */
    public static RateLimitCheckResult success(
            RateLimitRule rule,
            String key,
            RateLimiter.RateLimitResult result
    ) {
        return new RateLimitCheckResult(rule, key, result);
    }

    /**
     * Rate Limit 체크 실패 시 결과 생성 (Fail Open)
     */
    public static RateLimitCheckResult failure(
            RateLimitRule rule,
            String key
    ) {
        return new RateLimitCheckResult(rule, key, null);
    }

    /**
     * Rate Limit이 초과되었는지 확인합니다.
     */
    public boolean isExceeded() {
        return result != null && result.isExceeded();
    }

    /**
     * Rate Limit 체크가 실패했는지 확인합니다.
     */
    public boolean isFailed() {
        return result == null;
    }
}


