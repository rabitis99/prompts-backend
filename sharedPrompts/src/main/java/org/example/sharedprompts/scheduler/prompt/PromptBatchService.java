package org.example.sharedprompts.scheduler.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.comment.service.CommentCountService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptBatchService {

    private final CommentCountService commentCountService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
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
