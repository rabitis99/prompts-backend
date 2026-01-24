package org.example.sharedprompts.auth.rate.filter.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.filter.builder.writer.RateLimitResponseWriter;
import org.example.sharedprompts.auth.rate.filter.metrics.RateLimitMetricsCollector;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitKey;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.function.BiConsumer;

import static org.example.sharedprompts.auth.rate.policy.RateLimitConstants.Response;

/**
 * Rate Limit 초과 처리 Facade
 * 
 * Rate Limit 초과 시 메트릭, 로그, 응답 작성을 조율하는 Facade입니다.
 * 각 책임을 명확히 분리하여 유지보수성을 향상시켰습니다.
 */
@Component
@RequiredArgsConstructor
public class RateLimitExceededFacade {

    private final RateLimitMetricsCollector metricsCollector;
    private final ObjectMapper objectMapper;

    /**
     * Rate Limit 초과를 처리합니다.
     * 
     * 처리 순서:
     * 1. 메트릭 수집 (Observer 패턴)
     * 2. 로그 기록 (콜백을 통해)
     * 3. HTTP 응답 작성
     * 
     * @param rule RateLimitRule
     * @param key Rate Limit 키 (타입 정보 포함)
     * @param result RateLimitResult
     * @param response HttpServletResponse
     * @param logCallback 로그 기록 콜백 (key, result)
     *                    주의: 이 콜백은 handle() 메서드 내부의 recordLog()에서 호출됩니다.
     *                    AbstractRateLimitFilter에서는 이미 logRateLimitExceeded()를 통해
     *                    로깅이 완료되므로, 중복 로깅을 방지하기 위해 빈 람다 (k, r) -> {}를
     *                    전달하는 것이 일반적입니다. 필요시 추가적인 로깅이나 후처리를
     *                    수행할 수 있는 확장 포인트로 활용할 수 있습니다.
     * @param errorCode 사용할 에러 코드 (null이면 기본값 RATE_LIMIT_EXCEEDED 사용)
     * @throws IOException 응답 작성 실패 시
     */
    public void handle(
            RateLimitRule rule,
            RateLimitKey key,
            RateLimiter.RateLimitResult result,
            HttpServletResponse response,
            BiConsumer<RateLimitKey, RateLimiter.RateLimitResult> logCallback,
            ErrorCode errorCode
    ) throws IOException {
        long retryAfter = result.getRetryAfter(Response.MIN_RETRY_AFTER_SECONDS);

        // 1. 메트릭 수집 (Observer 패턴)
        recordMetrics(rule, result);

        // 2. 로그 기록 (콜백을 통해)
        recordLog(key, result, logCallback);

        // 3. HTTP 응답 작성
        writeResponse(response, retryAfter, errorCode);
    }

    /**
     * Rate Limit 초과를 처리합니다. (기본 에러 코드 사용)
     * 
     * @param rule RateLimitRule
     * @param key Rate Limit 키 (타입 정보 포함)
     * @param result RateLimitResult
     * @param response HttpServletResponse
     * @param logCallback 로그 기록 콜백 (key, result)
     *                    주의: 이 콜백은 handle() 메서드 내부의 recordLog()에서 호출됩니다.
     *                    AbstractRateLimitFilter에서는 이미 logRateLimitExceeded()를 통해
     *                    로깅이 완료되므로, 중복 로깅을 방지하기 위해 빈 람다 (k, r) -> {}를
     *                    전달하는 것이 일반적입니다. 필요시 추가적인 로깅이나 후처리를
     *                    수행할 수 있는 확장 포인트로 활용할 수 있습니다.
     * @throws IOException 응답 작성 실패 시
     */
    public void handle(
            RateLimitRule rule,
            RateLimitKey key,
            RateLimiter.RateLimitResult result,
            HttpServletResponse response,
            BiConsumer<RateLimitKey, RateLimiter.RateLimitResult> logCallback
    ) throws IOException {
        handle(rule, key, result, response, logCallback, null);
    }

    /**
     * 메트릭을 수집합니다.
     * 
     * @param rule RateLimitRule
     * @param result RateLimitResult
     */
    private void recordMetrics(RateLimitRule rule, RateLimiter.RateLimitResult result) {
        metricsCollector.recordExceeded(rule, result.currentCount(), rule.getCapacity());
    }

    /**
     * 로그를 기록합니다.
     * 
     * @param key Rate Limit 키
     * @param result RateLimitResult
     * @param logCallback 로그 기록 콜백
     */
    private void recordLog(
            RateLimitKey key,
            RateLimiter.RateLimitResult result,
            BiConsumer<RateLimitKey, RateLimiter.RateLimitResult> logCallback
    ) {
        logCallback.accept(key, result);
    }

    /**
     * HTTP 응답을 작성합니다.
     * 
     * @param response HttpServletResponse
     * @param retryAfter Retry-After 값 (초)
     * @param errorCode 사용할 에러 코드
     * @throws IOException 응답 작성 실패 시
     */
    private void writeResponse(
            HttpServletResponse response,
            long retryAfter,
            ErrorCode errorCode
    ) throws IOException {
        RateLimitResponseWriter.writeTooManyRequests(response, objectMapper, retryAfter, errorCode);
    }
}

