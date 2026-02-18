package org.example.sharedprompts.module.domain.production.service.ai.retry;

import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;

/**
 * 지수 백오프 재시도 정책
 * 5xx 에러만 재시도, 4xx는 재시도하지 않음
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
        if (attempt > maxRetries) {
            return false;
        }
        
        // 예외를 unwrap하여 WebClientResponseException 찾기
        WebClientResponseException webClientException = unwrapWebClientException(throwable);
        if (webClientException != null) {
            int statusCode = webClientException.getStatusCode().value();
            
            // 4xx 클라이언트 오류는 재시도하지 않음 (특히 403 FORBIDDEN)
            if (statusCode >= 400 && statusCode < 500) {
                // 403 FORBIDDEN의 경우, content filter 오류인지 확인
                if (statusCode == 403) {
                    String responseBody = webClientException.getResponseBodyAsString();
                    if (isContentFilterError(responseBody)) {
                        // Content filter 오류는 재시도해도 성공하지 않으므로 재시도하지 않음
                        return false;
                    }
                }
                // 다른 4xx 오류도 재시도하지 않음
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
    
    /**
     * 응답 본문이 content filter 오류인지 확인합니다.
     */
    private boolean isContentFilterError(String responseBody) {
        if (responseBody == null) {
            return false;
        }
        String lowerBody = responseBody.toLowerCase();
        return lowerBody.contains("filter") 
                && (lowerBody.contains("inappropriate") 
                    || lowerBody.contains("known person")
                    || lowerBody.contains("blocked"));
    }
    
    @Override
    public long calculateDelayMs(int attempt) {
        // 지수 백오프: initialDelay * 2^(attempt-1)
        return initialDelayMs * (1L << (attempt - 1));
    }
}

