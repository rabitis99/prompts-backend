package org.example.sharedprompts.domain.rate.ratelimitlog.service.impl;

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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitLogBatchServiceImpl implements RateLimitLogBatchService {

    private static final int BATCH_SIZE = 500;

    private final RateLimitLogRepository repository;
    private final RateLimitLogExceptionHandler exceptionHandler;

    @PersistenceContext
    private EntityManager entityManager;

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
        } catch (Exception e) {
            handleBatchSaveException(snapshot, e);
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
     * 배치 저장 중 발생한 예외를 처리합니다.
     * RateLimitLogExceptionHandler로 위임하고 예외를 재던져 AsyncUncaughtExceptionHandler가 감지할 수 있도록 합니다.
     *
     * @param snapshot 저장 시도한 로그 스냅샷
     * @param e 발생한 예외
     */
    private void handleBatchSaveException(List<RateLimitLog> snapshot, Exception e) {
        // 예외를 RateLimitLogExceptionHandler로 위임하여 중앙 집중식 처리
        // 배치 저장이므로 ruleName은 "BATCH", key는 null로 전달
        exceptionHandler.handleException("BATCH", null, e);
        // @Async 메서드에서 AsyncUncaughtExceptionHandler가 예외를 감지할 수 있도록 재던지기
        throw new RuntimeException("Failed to save rate limit logs in batch: count=" + snapshot.size(), e);
    }
}





