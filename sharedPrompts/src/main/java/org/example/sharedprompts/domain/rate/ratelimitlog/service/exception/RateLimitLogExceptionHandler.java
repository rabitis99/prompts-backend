package org.example.sharedprompts.domain.rate.ratelimitlog.service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Rate Limit 로그 예외 처리 핸들러
 * Rate Limit 로그 저장 시 발생하는 예외를 처리합니다.
 */
@Slf4j
@Component
public class RateLimitLogExceptionHandler {

    /**
     * 예외를 처리하고 로깅합니다.
     * 
     * @param ruleName 규칙 이름
     * @param key Rate Limit 키
     * @param exception 발생한 예외
     */
    public void handleException(String ruleName, String key, Exception exception) {

        if (exception instanceof DataAccessException) {
            // 데이터베이스 관련 예외
            log.error("Database error while saving rate limit log: rule={}, key={}", 
                    ruleName, key, exception);
        } else if (exception instanceof IllegalArgumentException) {
            // 잘못된 인자 예외
            log.error("Invalid argument while saving rate limit log: rule={}, key={}", 
                    ruleName, key, exception);
        } else {
            // 기타 예외
            log.error("Unexpected error while saving rate limit log: rule={}, key={}", 
                    ruleName, key, exception);
        }
    }
}