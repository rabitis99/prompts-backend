package org.example.sharedprompts.scheduler.like;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptLikeCountSyncScheduler {

    private final PromptRepository promptRepository;
    private final PromptLikeBatchService promptLikeBatchService;

    private static final int BATCH_SIZE = 1000;
    private static final long SCHEDULE_DELAY_MS = 10 * 60 * 1000L; // 10분

    @Scheduled(fixedDelay = SCHEDULE_DELAY_MS)
    @SchedulerLock(
            name = "PromptLikeCountSyncScheduler",
            lockAtMostFor = "15m",
            lockAtLeastFor = "1m"
    )
    public void syncPromptLikeCounts() {
        log.info("PromptLikeCountSyncScheduler started");

        Long lastId = 0L;
        List<Long> ids;

        do {
            ids = promptRepository.findAllIds(lastId, BATCH_SIZE);

            if (!ids.isEmpty()) {
                try {
                    promptLikeBatchService.processBatch(ids);
                } catch (Exception e) {
                    log.error("Failed to process like batch, ids={}", ids, e);
                }

                lastId = ids.get(ids.size() - 1);
            }
        } while (!ids.isEmpty());

        log.info("PromptLikeCountSyncScheduler finished");
    }
}
