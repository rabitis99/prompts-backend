package org.example.sharedprompts.scheduler.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptCountSyncScheduler {

    private final PromptRepository promptRepository;
    private final PromptBatchService promptBatchService;

    private static final int BATCH_SIZE = 1000;
    private static final long SCHEDULE_DELAY_MS = 10 * 60 * 1000L; // 10분

    @Scheduled(fixedDelay = SCHEDULE_DELAY_MS)
    @SchedulerLock(name = "PromptCountSyncScheduler", lockAtMostFor = "15m", lockAtLeastFor = "1m")
    @LockProviderToUse("fallbackLockProvider")
    public void syncPromptCommentCounts() {
        log.info("PromptCountSyncScheduler started");

        Long lastId = 0L;
        List<Long> ids;

        do {
            // lastId 기준 커서 페이징
            ids = promptRepository.findAllIds(lastId, BATCH_SIZE);

            if (!ids.isEmpty()) {
                try {
                    promptBatchService.processBatch(ids);
                } catch (Exception e) {
                    log.error("Failed to process batch, ids={}", ids, e);
                    // 실패 배치 별도 기록/재시도 가능
                }
                lastId = ids.get(ids.size() - 1);
            }
        } while (!ids.isEmpty());

        log.info("PromptCountSyncScheduler finished");
    }
}

