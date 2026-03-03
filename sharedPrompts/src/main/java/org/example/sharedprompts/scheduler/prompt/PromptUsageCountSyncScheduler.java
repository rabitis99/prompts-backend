package org.example.sharedprompts.scheduler.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.infrastructure.persistence.PromptRepository;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 프롬프트 조회수 동기화 스케줄러
 * 
 * <p>Redis에 누적된 조회수를 주기적으로 DB에 반영합니다.
 * <p>ShedLock을 사용하여 다중 인스턴스 환경에서도 중복 실행되지 않도록 합니다.
 * <p>FallbackLockProvider를 사용하여 Redis 장애 시에도 DB 기반 락으로 동작합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptUsageCountSyncScheduler {

    private final PromptRepository promptRepository;
    private final PromptUsageCountBatchService promptUsageCountBatchService;

    private static final int BATCH_SIZE = 1000;
    private static final long SCHEDULE_DELAY_MS = 10 * 60 * 1000L; // 10분

    /**
     * 주기적으로 Redis에 누적된 조회수를 DB에 반영
     * 
     * <p>실행 주기: 10분마다
     * <p>락 보유 시간: 최대 15분, 최소 1분
     * <p>FallbackLockProvider 사용: Redis 장애 시 DB 기반 락으로 전환
     */
    @Scheduled(fixedDelay = SCHEDULE_DELAY_MS)
    @SchedulerLock(
            name = "PromptUsageCountSyncScheduler",
            lockAtMostFor = "15m",
            lockAtLeastFor = "1m"
    )
    @LockProviderToUse("fallbackLockProvider")
    public void syncUsageCounts() {
        log.info("PromptUsageCountSyncScheduler started");

        Long lastId = 0L;
        List<Long> ids;
        int totalBatches = 0;
        int failedBatches = 0;

        do {
            ids = promptRepository.findAllIds(lastId, BATCH_SIZE);

            if (!ids.isEmpty()) {
                try {
                    promptUsageCountBatchService.processBatch(ids);
                    totalBatches++;
                } catch (Exception e) {
                    log.error("Failed to process usage count batch, ids={}", ids, e);
                    failedBatches++;
                }

                lastId = ids.get(ids.size() - 1);
            }
        } while (!ids.isEmpty());

        log.info("PromptUsageCountSyncScheduler finished: totalBatches={}, failedBatches={}", totalBatches, failedBatches);
    }
}

