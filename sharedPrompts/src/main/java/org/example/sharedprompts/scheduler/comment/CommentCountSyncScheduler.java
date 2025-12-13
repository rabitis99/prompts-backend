package org.example.sharedprompts.scheduler.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.comment.repository.CommentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentCountSyncScheduler {

    private final CommentRepository commentRepository;
    private final CommentBatchService commentBatchService;

    private static final int BATCH_SIZE = 1000;
    private static final long SCHEDULE_DELAY_MS = 10 * 60 * 1000L; // 10분

    @Scheduled(fixedDelay = SCHEDULE_DELAY_MS)
    @SchedulerLock(
            name = "CommentCountSyncScheduler",
            lockAtMostFor = "15m",
            lockAtLeastFor = "1m"
    )
    public void syncCommentCounts() {
        log.info("CommentCountSyncScheduler started");

        Long lastId = 0L;
        List<Long> ids;

        do {
            // 커서 기반 페이징
            ids = commentRepository.findAllIds(lastId, BATCH_SIZE);

            if (!ids.isEmpty()) {
                try {
                    commentBatchService.processBatch(ids);
                } catch (Exception e) {
                    log.error("Failed to process comment batch, ids={}", ids, e);
                }

                lastId = ids.get(ids.size() - 1);
            }
        } while (!ids.isEmpty());

        log.info("CommentCountSyncScheduler finished");
    }
}
