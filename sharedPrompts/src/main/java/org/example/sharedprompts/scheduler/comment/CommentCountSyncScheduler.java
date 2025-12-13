package org.example.sharedprompts.scheduler.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.comment.repository.CommentRepository;
import org.example.sharedprompts.domain.comment.service.CommentCountService;
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
public class CommentCountSyncScheduler {

    private final CommentRepository commentRepository; // -> Page<Long> findAllIds(Pageable)
    private final CommentCountService commentCountService;
    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;
    private static final long SCHEDULE_DELAY_MS = 10 * 60 * 1000L; // 10분

    /**
     * fixedDelay로 재진입 방지 (이전 실행 완료 후 delay 시작)
     * ShedLock으로 멀티 인스턴스 중 1개만 실행 보장
     */
    @Transactional
    @Scheduled(fixedDelay = SCHEDULE_DELAY_MS)
    @SchedulerLock(name = "CommentCountSyncScheduler", lockAtMostFor = "15m", lockAtLeastFor = "1m")
    public void syncCommentCounts() {
        log.info("CommentCountSyncScheduler started");
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
            log.info("CommentCountSyncScheduler finished");
        } catch (Exception e) {
            log.error("CommentCountSyncScheduler failed", e);
        }
    }

    /**
     * 한 배치(<= BATCH_SIZE)의 ID들에 대해 Redis에서 MGET으로 일괄 조회하고
     * JdbcTemplate.batchUpdate로 대량 반영.
     */
    @Transactional
    protected void processBatch(List<Long> ids) {
        if (ids.isEmpty()) return;

        // Redis에서 id 리스트에 대한 counts 조회
        Map<Long, Long> counts = commentCountService.getReplyCounts(ids); // 구현은 이미 안전하게 되어있음

        // Prepare batch args: List<Object[]>, 각 원소: {count, id}
        List<Object[]> batchArgs = ids.stream()
                .map(id -> new Object[]{counts.getOrDefault(id, 0L), id})
                .collect(Collectors.toList());

        // JdbcTemplate batch update: reply_count 컬럼을 id로 업데이트
        // (테이블/칼럼명은 실제 스키마에 맞게 조정)
        String sql = "UPDATE comments SET reply_count = ? WHERE id = ?";

        jdbcTemplate.batchUpdate(sql, batchArgs);

        log.debug("Processed batch size={}, sampleUpdated={}", ids.size(), Math.min(5, ids.size()));
    }
}
