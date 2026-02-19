package org.example.sharedprompts.module.domain.production.service.ai.retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.ai.exception.AiClientException;
import org.springframework.stereotype.Component;

/**
 * 재시도 로직을 실행하는 공통 유틸리티 클래스
 * AI 클라이언트들의 재시도 로직 중복을 제거합니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RetryExecutor {
    
    /**
     * 재시도 가능한 작업을 실행합니다.
     * 
     * @param operation 실행할 작업
     * @param retryPolicy 재시도 정책
     * @param operationName 작업 이름 (로깅용, 예: "Groq API call", "Leonardo API call")
     * @param <T> 반환 타입
     * @return 작업 결과
     * @throws AiClientException 모든 재시도 실패 시
     */
    public <T> T executeWithRetry(RetryableOperation<T> operation, RetryPolicy retryPolicy, String operationName) {
        int attempt = 0; // 0-based: 0 = 첫 번째 실패 후 첫 번째 재시도 전
        Exception lastException = null;
        
        // 최대 시도 횟수 = 초기 시도(1) + 재시도 횟수(maxRetries)
        int maxAttempts = retryPolicy.getMaxRetries() + 1;
        
        while (attempt < maxAttempts) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                
                // attempt는 0-based이므로, shouldRetry는 attempt < maxRetries인지 확인
                if (!retryPolicy.shouldRetry(attempt, e)) {
                    break;
                }
                
                long delayMs = retryPolicy.calculateDelayMs(attempt);
                log.warn("{} failed - attempt: {}/{}, retrying after {}ms. Error: {}", 
                        operationName, attempt + 1, maxAttempts, delayMs, e.getMessage());
                
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AiClientException("Retry interrupted", ie);
                }
                
                attempt++;
            }
        }
        
        // 모든 재시도 실패
        throw new AiClientException(
                String.format("%s failed after %d attempts", operationName, attempt + 1), 
                lastException);
    }
    
    /**
     * 재시도 가능한 작업 인터페이스
     */
    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}

