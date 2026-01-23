package org.example.sharedprompts.auth.rate.filter.service.check;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.filter.exception.RateLimitExceptionHandler;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitCheckResult;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.stereotype.Service;

/**
 * Rate Limit 체크 서비스
 * 
 * Rate Limit 체크 로직을 담당하는 서비스입니다.
 * 메트릭 수집은 Observer 패턴을 통해 분리되었습니다.
 */
@Service
@RequiredArgsConstructor
public class RateLimitCheckService {

    private final RateLimiter rateLimiter;
    private final RateLimitExceptionHandler exceptionHandler;

    /**
     * Rate Limit 체크를 수행합니다.
     * 
     * @param keyValue Rate Limit 키 값 (Redis 키로 사용)
     * @param rule RateLimitRule
     * @return RateLimitCheckResult (체크 결과 및 관련 정보)
     */
    public RateLimitCheckResult check(String keyValue, RateLimitRule rule) {
        try {
            RateLimiter.RateLimitResult result = rateLimiter.consume(
                    keyValue, rule.getCapacity(), rule.getWindowSeconds()
            );
            
            return RateLimitCheckResult.success(rule, keyValue, result);
            
        } catch (Exception e) {
            // 예외 처리 전략을 통해 처리
            return exceptionHandler.handleException(keyValue, rule, e);
        }
    }
}


