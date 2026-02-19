package org.example.sharedprompts.module.domain.production.service.ai.retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.ai.exception.AiClientException;
import org.springframework.stereotype.Component;

/**
 * 재시도 로직을 실행하는 공통 유틸리티 클래스
 * AI 클라이언트들의 재시도 로직 중복을 제거합니다.
 * 
 * <p><b>현재 구현:</b> 동기 블로킹 방식 (Thread.sleep 사용)
 * <ul>
 *   <li>현재 GroqTextAiClient 등에서 .block()을 사용하여 블로킹이 전제된 구조</li>
 *   <li>요청 스레드에서 호출될 경우 Thread.sleep()으로 인해 스레드 풀이 고갈될 수 있음</li>
 * </ul>
 * 
 * <p><b>향후 개선 방향:</b> 비동기 전환 시 리액티브 재시도로 전환 고려
 * <ul>
 *   <li>Mono.delay() 기반의 리액티브 재시도 패턴 사용 (예: retryWhen(Retry.backoff(...)))</li>
 *   <li>GoogleGeminiService의 retryWhen 패턴 참고</li>
 *   <li>요청 스레드 블로킹 없이 비동기적으로 재시도 수행</li>
 * </ul>
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
        int totalAttempts = 0; // 실제 시도 횟수 추적
        Exception lastException = null;
        
        // 최대 시도 횟수 = 초기 시도(1) + 재시도 횟수(maxRetries)
        int maxAttempts = retryPolicy.getMaxRetries() + 1;
        
        while (attempt < maxAttempts) {
            try {
                totalAttempts++;
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
                
                // TODO: 향후 비동기 전환 시 Mono.delay() 기반 리액티브 재시도로 전환
                // 현재는 블로킹 구조(.block() 사용)이므로 Thread.sleep() 사용
                // 비동기 전환 시: retryWhen(Retry.backoff(...)) 패턴 사용 권장
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
                String.format("%s failed after %d attempts", operationName, totalAttempts), 
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

