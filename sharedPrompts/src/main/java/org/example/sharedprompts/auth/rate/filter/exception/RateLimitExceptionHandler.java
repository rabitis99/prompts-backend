package org.example.sharedprompts.auth.rate.filter.exception;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimitException;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitCheckResult;
import org.example.sharedprompts.auth.rate.filter.metrics.RateLimitMetricsCollector;
import org.example.sharedprompts.auth.rate.filter.service.logging.RateLimitLoggingService;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;

/**
 * Rate Limit 예외 처리 핸들러
 * 
 * Rate Limit 체크 중 발생하는 예외를 처리하는 전략 클래스입니다.
 */
@Component
@RequiredArgsConstructor
public class RateLimitExceptionHandler {

    private final RateLimitLoggingService loggingService;
    private final RateLimitMetricsCollector metricsCollector;

    /**
     * 예외를 처리하고 RateLimitCheckResult를 반환합니다.
     * 
     * @param key Rate Limit 키
     * @param rule RateLimitRule
     * @param exception 발생한 예외
     * @return RateLimitCheckResult (Fail Open 정책에 따라 항상 허용)
     */
    public RateLimitCheckResult handleException(String key, RateLimitRule rule, Exception exception) {
        String errorMessage = buildErrorMessage(exception);
        loggingService.logRateLimitCheckFailed(key, errorMessage, exception);
        metricsCollector.recordFailed(rule);
        return RateLimitCheckResult.failure(rule, key);
    }

    /**
     * 예외 타입에 따라 적절한 에러 메시지를 생성합니다.
     * 
     * @param exception 발생한 예외
     * @return 에러 메시지
     */
    private String buildErrorMessage(Exception exception) {
        if (exception instanceof RateLimitException) {
            return exception.getMessage();
        } else if (exception instanceof RedisConnectionFailureException) {
            return "Redis connection failed: " + exception.getMessage();
        } else {
            return "Unexpected error: " + exception.getMessage();
        }
    }
}

