package org.example.sharedprompts.scheduler.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.comment.service.CommentCountService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptCountSyncScheduler {

    private final PromptRepository promptRepository; // -> Page<Long> findAllIds(Pageable)
    private final CommentCountService commentCountService;
    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;
    private static final long SCHEDULE_DELAY_MS = 10 * 60 * 1000L; // 10분

    @Scheduled(fixedDelay = SCHEDULE_DELAY_MS)
    @SchedulerLock(name = "PromptCountSyncScheduler", lockAtMostFor = "15m", lockAtLeastFor = "1m")
    public void syncPromptCommentCounts() {
        log.info("PromptCountSyncScheduler started");
        int page = 0;
        try {
            Page<Long> pageIds;
            do {
                pageIds = promptRepository.findAllIds(PageRequest.of(page, BATCH_SIZE));
                List<Long> ids = pageIds.getContent();
                if (!ids.isEmpty()) {
                    processBatch(ids);
                }
                page++;
            } while (pageIds.hasNext());
            log.info("PromptCountSyncScheduler finished");
        } catch (Exception e) {
            log.error("PromptCountSyncScheduler failed", e);
        }
    }

    protected void processBatch(List<Long> ids) {
        if (ids.isEmpty()) return;

        Map<Long, Long> counts = commentCountService.getCommentCounts(ids);

        List<Object[]> batchArgs = ids.stream()
                .map(id -> new Object[]{counts.getOrDefault(id, 0L), id})
                .collect(Collectors.toList());

        // Prompt 테이블의 루트 댓글 수 컬럼에 저장 (컬럼명은 스키마에 맞게 조정)
        String sql = "UPDATE prompts SET comment_count = ? WHERE id = ?";

        jdbcTemplate.batchUpdate(sql, batchArgs);

        log.debug("Processed prompt batch size={}, sampleUpdated={}", ids.size(), Math.min(5, ids.size()));
    }
}
