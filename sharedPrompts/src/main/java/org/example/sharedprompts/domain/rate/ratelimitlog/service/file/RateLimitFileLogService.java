package org.example.sharedprompts.domain.rate.ratelimitlog.service.file;

import jakarta.servlet.http.HttpServletRequest;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;

/**
 * Rate Limit 파일 로그 서비스 인터페이스
 * 
 * Rate Limit 관련 로그를 파일에 기록하는 서비스입니다.
 * 파일 로그는 동기적으로 기록되어 즉시 확인 가능합니다.
 */
public interface RateLimitFileLogService {

    /**
     * Rate Limit 초과 로그를 파일에 기록합니다.
     * 
     * @param rule RateLimitRule
     * @param key Rate Limit 키
     * @param result RateLimitResult
     * @param request HttpServletRequest
     * @param userId 사용자 ID (IP 기반인 경우 null)
     */
    void logRateLimitExceeded(
            RateLimitRule rule,
            String key,
            RateLimiter.RateLimitResult result,
            HttpServletRequest request,
            Long userId
    );

    /**
     * Rate Limit 체크 실패 로그를 기록합니다.
     * 
     * @param key Rate Limit 키
     * @param error 예외 메시지
     * @param exception 예외 객체
     */
    void logRateLimitCheckFailed(String key, String error, Exception exception);

    /**
     * 잘못된 HTTP Method 로그를 기록합니다.
     * 
     * @param method HTTP Method 문자열
     */
    void logInvalidHttpMethod(String method);
}
