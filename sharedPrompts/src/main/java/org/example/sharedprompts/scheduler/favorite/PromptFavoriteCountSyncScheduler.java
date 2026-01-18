package org.example.sharedprompts.scheduler.favorite;

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
public class PromptFavoriteCountSyncScheduler {

    private final PromptRepository promptRepository;
    private final PromptFavoriteBatchService promptFavoriteBatchService;

    private static final int BATCH_SIZE = 1000;
    private static final long SCHEDULE_DELAY_MS = 10 * 60 * 1000L; // 10분

    @Scheduled(fixedDelay = SCHEDULE_DELAY_MS)
    @SchedulerLock(
            name = "PromptFavoriteCountSyncScheduler",
            lockAtMostFor = "15m",
            lockAtLeastFor = "1m"
    )
    public void syncPromptFavoriteCounts() {
        log.info("PromptFavoriteCountSyncScheduler started");

        Long lastId = 0L;
        List<Long> ids;
        int totalBatches = 0;
        int failedBatches = 0;

        do {
            ids = promptRepository.findAllIds(lastId, BATCH_SIZE);

            if (!ids.isEmpty()) {
                try {
                    promptFavoriteBatchService.processBatch(ids);
                    totalBatches++;
                } catch (Exception e) {
                    log.error("Failed to process favorite batch, ids={}", ids, e);
                    failedBatches++;
                }

                lastId = ids.get(ids.size() - 1);
            }
        } while (!ids.isEmpty());

        log.info("PromptFavoriteCountSyncScheduler finished: totalBatches={}, failedBatches={}", totalBatches, failedBatches);
    }
}

