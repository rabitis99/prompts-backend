package org.example.sharedprompts.auth.rate.filter.util;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;

import java.time.Instant;

/**
 * RateLimit HTTP 헤더 유틸리티
 * 
 * 요구사항에 맞게 RateLimit 관련 HTTP 헤더를 설정합니다.
 * - X-RateLimit-Limit: 시간 윈도우당 최대 요청 수
 * - X-RateLimit-Remaining: 남은 요청 수
 * - X-RateLimit-Reset: 윈도우 리셋 시간 (Unix timestamp, 초 단위)
 * - Retry-After: 재시도 가능한 시간 (429 에러 시)
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RateLimitHeaderUtil {

    private static final String HEADER_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    /**
     * RateLimit 헤더를 응답에 추가합니다.
     * 
     * @param response HttpServletResponse
     * @param rule RateLimitRule
     * @param result RateLimitResult (null 가능 - 헤더만 추가)
     */
    public static void addRateLimitHeaders(
            HttpServletResponse response,
            RateLimitRule rule,
            RateLimiter.RateLimitResult result
    ) {
        long limit = rule.getCapacity();
        long remaining = result != null ? Math.max(0, limit - result.currentCount()) : limit;
        long resetTimestamp = calculateResetTimestamp(rule, result);

        response.setHeader(HEADER_RATE_LIMIT_LIMIT, String.valueOf(limit));
        response.setHeader(HEADER_RATE_LIMIT_REMAINING, String.valueOf(remaining));
        response.setHeader(HEADER_RATE_LIMIT_RESET, String.valueOf(resetTimestamp));
    }

    /**
     * RateLimit 초과 시 헤더를 추가합니다 (429 에러용).
     * 
     * @param response HttpServletResponse
     * @param rule RateLimitRule
     * @param result RateLimitResult
     */
    public static void addRateLimitExceededHeaders(
            HttpServletResponse response,
            RateLimitRule rule,
            RateLimiter.RateLimitResult result
    ) {
        addRateLimitHeaders(response, rule, result);

        long retryAfter = result.getRetryAfter(1L);
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfter));
    }

    private static long calculateResetTimestamp(
            RateLimitRule rule,
            RateLimiter.RateLimitResult result
    ) {
        long now = Instant.now().getEpochSecond();
        
        if (result != null) {
            if (result.retryAfterSeconds() > 0) {
                return now + result.retryAfterSeconds();
            }
            if (result.ttlSeconds() > 0) {
                return now + result.ttlSeconds();
            }
        }
        
        return now + rule.getWindowSeconds();
    }
}


