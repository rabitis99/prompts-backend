package org.example.sharedprompts.domain.rate.ratelimitlog.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.rate.ratelimitlog.RateLimitLog;
import org.example.sharedprompts.domain.rate.ratelimitlog.repository.RateLimitLogRepository;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.RateLimitLogBatchService;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.exception.RateLimitLogExceptionHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Rate Limit 로그 배치 저장 서비스 구현체
 * 
 * <p>비동기로 Rate Limit 로그를 배치 저장하며, 실패 시 메트릭 기록 및 예외 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitLogBatchServiceImpl implements RateLimitLogBatchService {

    private static final int BATCH_SIZE = 500;

    private final RateLimitLogRepository repository;
    private final RateLimitLogExceptionHandler exceptionHandler;
    private final MeterRegistry meterRegistry;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Persist a list of RateLimitLog entities in fixed-size batches and record success/failure metrics.
     *
     * <p>If {@code logs} is null or empty this method returns immediately. The method creates a
     * shallow snapshot of the input list, saves entities in chunks (defined by {@code BATCH_SIZE}),
     * flushes and clears the persistence context between chunks, and updates Micrometer counters and
     * summaries for success or failure. If an exception occurs, the snapshot and exception are
     * delegated to the central exception handler and a RuntimeException is thrown to surface the
     * failure to the async uncaught exception handler.
     *
     * @param logs the logs to persist; may be null or empty
     */
    @Override
    @Async("rateLimitLogTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveBatch(List<RateLimitLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        List<RateLimitLog> snapshot = new ArrayList<>(logs);

        try {
            int totalSaved = saveInChunks(snapshot);
            log.debug("Saved {} rate limit logs in batch (chunk size: {})", totalSaved, BATCH_SIZE);
            
            // 배치 작업 성공 횟수 기록
            meterRegistry.counter("rate_limit_log.batch.save", "result", "success").increment();
            // 저장된 로그 수 기록 (DistributionSummary로 평균, 최대, 최소 등 통계 추적)
            meterRegistry.summary("rate_limit_log.batch.save.size", "result", "success")
                    .record(totalSaved);
        } catch (Exception e) {
            // 배치 작업 실패 횟수 기록
            meterRegistry.counter("rate_limit_log.batch.save", "result", "failure").increment();
            // 실패한 로그 수 기록
            meterRegistry.summary("rate_limit_log.batch.save.size", "result", "failure")
                    .record(snapshot.size());
            
            handleBatchSaveException(snapshot, e);
            // 예외 재던지기 - AsyncUncaughtExceptionHandler가 처리
            throw new RuntimeException("Failed to save rate limit logs in batch: count=" + snapshot.size(), e);
        }
    }

    /**
     * 대량 리스트를 배치 청크로 나누어 저장합니다.
     * 메모리/타임아웃 위험을 방지하기 위해 청크 단위로 처리합니다.
     *
     * @param logs 저장할 로그 리스트
     * @return 저장된 총 로그 개수
     */
    private int saveInChunks(List<RateLimitLog> logs) {
        int totalSaved = 0;
        for (int i = 0; i < logs.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, logs.size());
            List<RateLimitLog> chunk = logs.subList(i, end);
            repository.saveAll(chunk);
            entityManager.flush();
            entityManager.clear();
            totalSaved += chunk.size();
        }
        return totalSaved;
    }

    /**
     * Delegates exceptions that occur during batch saving to the central RateLimitLogExceptionHandler.
     *
     * <p>This method forwards the error with ruleName "BATCH" and a null key; it does not rethrow the exception.
     *
     * @param snapshot the list of logs that were being saved when the exception occurred
     * @param e the exception that was raised during batch save
     */
    private void handleBatchSaveException(List<RateLimitLog> snapshot, Exception e) {
        // 예외를 RateLimitLogExceptionHandler로 위임하여 중앙 집중식 처리
        // 배치 저장이므로 ruleName은 "BATCH", key는 null로 전달
        exceptionHandler.handleException("BATCH", null, e);
    }
}




