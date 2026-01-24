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
                    ruleName, maskKey(key), exception);
        } else if (exception instanceof IllegalArgumentException) {
            // 잘못된 인자 예외
            log.error("Invalid argument while saving rate limit log: rule={}, key={}",
                    ruleName, maskKey(key), exception);
        } else {
            // 기타 예외
            log.error("Unexpected error while saving rate limit log: rule={}, key={}",
                    ruleName, maskKey(key), exception);
        }
    }

    /**
     * 키를 마스킹하여 PII 노출 방지
     *
     * @param key 원본 키
     * @return 마스킹 처리된 키
     */
    private String maskKey(String key) {
        if (key == null) return "null";
        int keep = Math.min(4, key.length());  // 앞 4자리만 유지
        return key.substring(0, keep) + "***";
    }
}
