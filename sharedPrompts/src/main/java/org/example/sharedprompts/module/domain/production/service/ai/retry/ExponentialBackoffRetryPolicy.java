package org.example.sharedprompts.module.domain.production.service.ai.retry;

import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;

/**
 * 지수 백오프 재시도 정책
 * 
 * <p>재시도 대상:
 * <ul>
 *   <li>429 Too Many Requests (Rate Limit) - 재시도</li>
 *   <li>5xx 서버 오류 - 재시도</li>
 *   <li>네트워크 오류 (IOException) - 재시도</li>
 * </ul>
 * 
 * <p>재시도하지 않음:
 * <ul>
 *   <li>모든 4xx 클라이언트 오류 (429 제외) - 재시도하지 않음</li>
 * </ul>
 */
@RequiredArgsConstructor
public class ExponentialBackoffRetryPolicy implements RetryPolicy {
    
    private final int maxRetries;
    private final long initialDelayMs;
    
    @Override
    public int getMaxRetries() {
        return maxRetries;
    }
    
    @Override
    public boolean shouldRetry(int attempt, Throwable throwable) {
        // attempt는 0-based이므로, attempt >= maxRetries이면 재시도하지 않음
        if (attempt >= maxRetries) {
            return false;
        }
        
        // 예외를 unwrap하여 WebClientResponseException 찾기
        WebClientResponseException webClientException = unwrapWebClientException(throwable);
        if (webClientException != null) {
            int statusCode = webClientException.getStatusCode().value();
            
            // 429 Too Many Requests는 Rate Limit 오류이므로 재시도
            if (statusCode == 429) {
                return true;
            }
            
            // 4xx 클라이언트 오류는 재시도하지 않음 (429 제외)
            if (statusCode >= 400 && statusCode < 500) {
                return false;
            }
            
            // 5xx 서버 오류만 재시도
            return statusCode >= 500 && statusCode < 600;
        }
        
        // 네트워크 오류 등은 재시도
        return throwable instanceof IOException;
    }
    
    /**
     * 예외 체인을 따라가며 WebClientResponseException을 찾습니다.
     */
    private WebClientResponseException unwrapWebClientException(Throwable throwable) {
        Throwable current = throwable;
        int depth = 0;
        final int maxDepth = 10; // 무한 루프 방지
        
        while (current != null && depth < maxDepth) {
            if (current instanceof WebClientResponseException) {
                return (WebClientResponseException) current;
            }
            current = current.getCause();
            depth++;
        }
        return null;
    }
    
    
    @Override
    public long calculateDelayMs(int attempt) {
        // 방어 코드: attempt가 0 이하이면 기본 지연 시간 반환
        if (attempt <= 0) {
            return initialDelayMs;
        }
        
        // 지수 백오프: initialDelay * 2^attempt (attempt는 0-based)
        // attempt = 0: initialDelay * 2^0 = initialDelay (방어 코드로 처리)
        // attempt = 1: initialDelay * 2^1 = initialDelay * 2
        // attempt = 2: initialDelay * 2^2 = initialDelay * 4
        
        // 시프트 연산 오버플로우 방지: attempt가 63 이상이면 Long.MAX_VALUE 반환
        if (attempt >= 63) {
            return Long.MAX_VALUE;
        }
        
        long shift = 1L << attempt;
        // 곱셈 오버플로우 방지: initialDelayMs * shift가 Long.MAX_VALUE를 초과하지 않도록 검증
        if (initialDelayMs > 0 && shift > Long.MAX_VALUE / initialDelayMs) {
            return Long.MAX_VALUE;
        }
        return initialDelayMs * shift;
    }
}

