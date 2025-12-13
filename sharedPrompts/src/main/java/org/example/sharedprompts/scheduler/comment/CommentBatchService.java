package org.example.sharedprompts.scheduler.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.comment.service.CommentCountService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentBatchService {

    private final CommentCountService commentCountService;
    private final JdbcTemplate jdbcTemplate;

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
