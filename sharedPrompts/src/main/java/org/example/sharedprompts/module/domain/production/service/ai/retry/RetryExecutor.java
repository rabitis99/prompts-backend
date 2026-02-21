package org.example.sharedprompts.module.domain.production.service.ai.retry;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.ai.exception.AiClientException;
import org.springframework.stereotype.Component;

/**
 * AI 클라이언트 인프라 레벨 재시도 실행기
 *
 * [P0-2 현황 및 한계]
 * - 이 클래스는 consumer 스레드를 blocking 함 (Thread.sleep 사용)
 * - 완전한 non-blocking 전환은 AI 처리 파이프라인 전체의 reactive 전환이 필요 (P2-1)
 *
 * [현재 완화 조치]
 * 1. application.yml max-retries=2 (기본 3→2로 감소, 최대 blocking 시간 단축)
 * 2. backoff 상한 3000ms (P0-2 min fix 적용)
 * 3. 최종 실패 시 message-level retry (ReactiveJobRetryService)로 위임 (P0-5 적용)
 *
 * [P2-1 구조적 목표]
 * Consumer는 외부 호출 실패 시 재시도 대기하지 않고 즉시 반환.
 * 모든 retry는 메시지 레벨(retry queue)로 위임.
 * 구현: max-retries=0 설정 + ReactiveJobRetryService가 전체 retry 담당
 */
@Component
@Slf4j
public class RetryExecutor {

    public <T> T executeWithRetry(RetryableOperation<T> operation, RetryPolicy retryPolicy, String operationName) {
        int attempt = 0;
        int totalAttempts = 0;
        Exception lastException = null;

        int maxAttempts = retryPolicy.getMaxRetries() + 1;

        while (attempt < maxAttempts) {
            try {
                totalAttempts++;
                return operation.execute();
            } catch (Exception e) {
                lastException = e;

                if (!retryPolicy.shouldRetry(attempt, e)) {
                    break;
                }

                long delayMs = retryPolicy.calculateDelayMs(attempt);
                log.warn("{} failed - attempt: {}/{}, retrying after {}ms. Error: {}",
                        operationName, attempt + 1, maxAttempts, delayMs, e.getMessage());

                // [P0-2] Consumer 스레드 blocking 발생 지점
                // 이 sleep이 consumer thread를 점유 (최대 maxRetries × maxBackoff = 2 × 3000 = 6초)
                // 완전 제거는 P2-1 reactive pipeline 전환 시 수행
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AiClientException("Retry interrupted", ie);
                }

                attempt++;
            }
        }

        throw new AiClientException(
                String.format("%s failed after %d attempts", operationName, totalAttempts),
                lastException);
    }

    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}
