package org.example.sharedprompts.auth.rate.filter.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;

/**
 * Rate Limit 결과와 키를 함께 담는 객체
 * 
 * Rate Limit 체크 결과와 키 정보를 함께 전달하여
 * Filter의 책임을 줄이고 Processor의 결과를 명확하게 표현합니다.
 */
@Getter
@RequiredArgsConstructor
public class RateLimitResultWithKey {
    
    private final RateLimitRule rule;
    private final RateLimitKey key;
    private final RateLimitCheckResult checkResult;

    /**
     * Rate Limit 결과를 생성합니다.
     * 
     * @param rule RateLimitRule
     * @param key RateLimitKey
     * @param checkResult RateLimitCheckResult
     * @return RateLimitResultWithKey
     */
    public static RateLimitResultWithKey of(
            RateLimitRule rule,
            RateLimitKey key,
            RateLimitCheckResult checkResult
    ) {
        return new RateLimitResultWithKey(rule, key, checkResult);
    }

    /**
     * Rate Limit이 초과되었는지 확인합니다.
     */
    public boolean isExceeded() {
        return checkResult != null && checkResult.isExceeded();
    }

    /**
     * Rate Limit 체크가 실패했는지 확인합니다.
     */
    public boolean isFailed() {
        return checkResult == null || checkResult.isFailed();
    }

    /**
     * RateLimitResult를 반환합니다.
     */
    public RateLimiter.RateLimitResult getResult() {
        return checkResult != null ? checkResult.getResult() : null;
    }
}





