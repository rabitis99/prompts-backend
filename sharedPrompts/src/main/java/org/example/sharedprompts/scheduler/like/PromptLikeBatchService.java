package org.example.sharedprompts.scheduler.like;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.like.service.LikeCountService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptLikeBatchService {

    private final LikeCountService likeCountService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void processBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;

        Map<Long, Long> counts = likeCountService.getPromptLikeCounts(ids);

        List<Object[]> batchArgs = ids.stream()
                .filter(counts::containsKey)
                .map(id -> new Object[]{counts.getOrDefault(id, 0L), id})
                .collect(Collectors.toList());

        String sql = "UPDATE prompts SET like_count = ? WHERE id = ?";

        jdbcTemplate.batchUpdate(sql, batchArgs);

        log.debug("Processed prompt-like batch size={}, sampleUpdated={}", ids.size(), Math.min(5, ids.size()));
    }
}
