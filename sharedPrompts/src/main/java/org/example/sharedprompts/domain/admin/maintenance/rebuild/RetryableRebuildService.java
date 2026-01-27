package org.example.sharedprompts.domain.admin.maintenance.rebuild;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * 재시도 메커니즘을 제공하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryableRebuildService {
    
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 1000; // 1초
    
    /**
     * 재시도 가능한 작업 실행
     * @param operation 실행할 작업
     * @param maxRetries 최대 재시도 횟수
     * @param retryDelayMs 재시도 간 지연 시간 (밀리초)
     * @return 작업 결과
     */
    public <T> T executeWithRetry(Supplier<T> operation, int maxRetries, long retryDelayMs) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt < maxRetries) {
                    log.warn("작업 실패 (시도 {}/{}), {}ms 후 재시도: {}", 
                            attempt, maxRetries, retryDelayMs, e.getMessage());
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                    }
                } else {
                    log.error("작업 실패 (최대 재시도 횟수 초과): {}", e.getMessage(), e);
                }
            }
        }
        
        throw new RuntimeException("최대 재시도 횟수 초과", lastException);
    }
    
    /**
     * 기본 설정으로 재시도 가능한 작업 실행
     */
    public <T> T executeWithRetry(Supplier<T> operation) {
        return executeWithRetry(operation, DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY_MS);
    }
    
    /**
     * 재시도 불가능한 작업 실행 (예외를 그대로 전파)
     */
    public <T> T execute(Supplier<T> operation) {
        return operation.get();
    }
}

