package org.example.sharedprompts.domain.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * SSE 배치 전송 서비스
 * - 여러 사용자에게 동시에 SSE 알림 전송
 * - 전송 결과 통계 제공
 * - 병렬 처리로 컨슈머 스레드 블로킹 방지
 */
@Slf4j
@Component
public class SseBatchSender {

    private final NotificationSseService sseService;
    private final Executor sseTaskExecutor;

    public SseBatchSender(
            NotificationSseService sseService,
            @Qualifier("sseTaskExecutor") Executor sseTaskExecutor) {
        this.sseService = sseService;
        this.sseTaskExecutor = sseTaskExecutor;
    }

    /**
     * 여러 사용자에게 SSE 알림 배치 전송
     * - 병렬 처리로 컨슈머 스레드 블로킹 방지
     * - 느린 클라이언트가 있어도 전체 처리 시간 단축
     *
     * @param userIds 알림 대상 사용자 ID 리스트
     * @param payload 전송할 페이로드
     * @return 전송 결과 통계
     */
    public SseSendResult sendBatch(List<Long> userIds, Object payload) {
        if (userIds == null || userIds.isEmpty()) {
            return SseSendResult.empty();
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        Exception[] lastException = new Exception[1];

        // 각 SSE 전송을 비동기로 실행
        List<CompletableFuture<Void>> futures = userIds.stream()
                .map(userId -> CompletableFuture.runAsync(() -> {
                    try {
                        sseService.sendPromptCreated(userId, payload);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        lastException[0] = e;
                        log.warn("Failed to send SSE to user {}: {}", userId, e.getMessage());
                    }
                }, sseTaskExecutor))
                .collect(Collectors.toList());

        // 모든 전송 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return new SseSendResult(successCount.get(), failureCount.get(), lastException[0]);
    }

    /**
     * SSE 전송 결과 통계
     */
    public static class SseSendResult {
        private final int successCount;
        private final int failureCount;
        private final Exception lastException;

        public SseSendResult(int successCount, int failureCount, Exception lastException) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.lastException = lastException;
        }

        public static SseSendResult empty() {
            return new SseSendResult(0, 0, null);
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public int getTotalCount() {
            return successCount + failureCount;
        }

        public boolean isAllFailed() {
            return failureCount > 0 && successCount == 0;
        }

        public boolean isAllSucceeded() {
            return failureCount == 0 && successCount > 0;
        }

        public boolean hasPartialFailure() {
            return successCount > 0 && failureCount > 0;
        }

        public Exception getLastException() {
            return lastException;
        }
    }
}

