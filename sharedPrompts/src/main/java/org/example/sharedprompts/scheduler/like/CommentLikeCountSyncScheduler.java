package org.example.sharedprompts.scheduler.like;

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
public class CommentLikeCountSyncScheduler {

    private final CommentRepository commentRepository;
    private final CommentLikeBatchService commentLikeBatchService;

    private static final int BATCH_SIZE = 1000;
    private static final long SCHEDULE_DELAY_MS = 10 * 60 * 1000L; // 10분

    @Scheduled(fixedDelay = SCHEDULE_DELAY_MS)
    @SchedulerLock(
            name = "CommentLikeCountSyncScheduler",
            lockAtMostFor = "15m",
            lockAtLeastFor = "1m"
    )
    public void syncCommentLikeCounts() {
        log.info("CommentLikeCountSyncScheduler started");

        Long lastId = 0L;
        List<Long> ids;
        int totalBatches = 0;
        int failedBatches = 0;

        do {
            // 커서 기반 페이징 (offset 제거)
            ids = commentRepository.findAllIds(lastId, BATCH_SIZE);

            if (!ids.isEmpty()) {
                try {
                    commentLikeBatchService.processBatch(ids);
                    totalBatches++;
                } catch (Exception e) {
                    log.error("Failed to process comment like batch, ids={}", ids, e);
                    failedBatches++;
                }

                lastId = ids.get(ids.size() - 1);
            }
        } while (!ids.isEmpty());

        log.info("CommentLikeCountSyncScheduler finished: totalBatches={}, failedBatches={}", totalBatches, failedBatches);
    }
}
