package org.example.sharedprompts.scheduler.like;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.comment.repository.CommentRepository;
import org.example.sharedprompts.domain.like.service.LikeCountService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentLikeCountSyncScheduler {

    private final CommentRepository commentRepository;
    private final LikeCountService likeCountService;
    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;
    private static final long SCHEDULE_DELAY_MS = 10 * 60 * 1000L; // 10분

    @Transactional
    @Scheduled(fixedDelay = SCHEDULE_DELAY_MS)
    @SchedulerLock(name = "CommentLikeCountSyncScheduler", lockAtMostFor = "15m", lockAtLeastFor = "1m")
    public void syncCommentLikeCounts() {
        log.info("CommentLikeCountSyncScheduler started");
        int page = 0;
        try {
            Page<Long> pageIds;
            do {
                pageIds = commentRepository.findAllIds(PageRequest.of(page, BATCH_SIZE));
                List<Long> ids = pageIds.getContent();
                if (!ids.isEmpty()) {
                    processBatch(ids);
                }
                page++;
            } while (pageIds.hasNext());
            log.info("CommentLikeCountSyncScheduler finished");
        } catch (Exception e) {
            log.error("CommentLikeCountSyncScheduler failed", e);
        }
    }

    @Transactional
    protected void processBatch(List<Long> ids) {
        if (ids.isEmpty()) return;

        Map<Long, Long> counts = likeCountService.getCommentLikeCounts(ids);

        List<Object[]> batchArgs = ids.stream()
                .map(id -> new Object[]{counts.getOrDefault(id, 0L), id})
                .collect(Collectors.toList());

        String sql = "UPDATE comments SET like_count = ? WHERE id = ?";

        jdbcTemplate.batchUpdate(sql, batchArgs);

        log.debug("Processed batch size={}, sampleUpdated={}", ids.size(), Math.min(5, ids.size()));
    }
}
