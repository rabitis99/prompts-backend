package org.example.sharedprompts.scheduler.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.service.PromptUsageCountService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 프롬프트 조회수 배치 처리 서비스
 * 
 * <p>Redis에 누적된 조회수를 DB에 반영하고, 반영 후 Redis 값을 초기화합니다.
 * <p>DB 반영 실패 시 Redis 값은 유지하여 다음 스케줄에서 재시도할 수 있도록 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptUsageCountBatchService {

    private final PromptUsageCountService promptUsageCountService;
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    private static final String USAGE_KEY_PREFIX = "prompt:usage:";

    /**
     * 프롬프트 ID 목록에 대한 조회수를 배치로 처리
     * 
     * <p>1. Redis에서 누적된 조회수 조회
     * <p>2. DB의 view_count에 batch로 합산
     * <p>3. 반영 성공 시 Redis 값 초기화
     * <p>4. 반영 실패 시 Redis 값 유지 (다음 스케줄에서 재시도)
     * 
     * @param promptIds 처리할 프롬프트 ID 목록
     */
    @Transactional
    protected void processBatch(List<Long> promptIds) {
        if (promptIds == null || promptIds.isEmpty()) {
            return;
        }

        try {
            // 1. Redis에서 누적된 조회수 조회
            Map<Long, Long> usageCounts = promptUsageCountService.getUsageCounts(promptIds);

            // 2. 조회수가 있는 프롬프트만 필터링하여 배치 업데이트 준비
            List<Object[]> batchArgs = promptIds.stream()
                    .filter(usageCounts::containsKey)
                    .filter(id -> usageCounts.get(id) > 0) // 0보다 큰 값만 처리
                    .map(id -> new Object[]{usageCounts.get(id), id})
                    .collect(Collectors.toList());

            if (batchArgs.isEmpty()) {
                return;
            }

            // 3. DB의 view_count에 batch로 합산
            String sql = "UPDATE prompts SET view_count = view_count + ? WHERE id = ?";
            jdbcTemplate.batchUpdate(sql, batchArgs);

            log.debug("Processed usage count batch: requested={}, updated={}", promptIds.size(), batchArgs.size());

            // 4. 반영 성공 시 Redis 값 초기화 (DELETE)
            for (Long promptId : usageCounts.keySet()) {
                try {
                    String key = USAGE_KEY_PREFIX + promptId;
                    redisTemplate.delete(key);
                } catch (Exception e) {
                    // Redis 삭제 실패는 로그만 남기고 계속 진행
                    // 다음 스케줄에서 재시도할 수 있도록 함
                    log.warn("Failed to delete Redis key after DB sync. promptId={}, key={}, error={}", 
                            promptId, USAGE_KEY_PREFIX + promptId, e.getMessage());
                }
            }

        } catch (Exception e) {
            // DB 반영 실패 시 Redis 값은 유지하여 다음 스케줄에서 재시도 가능
            log.error("Failed to process usage count batch. promptIds={}, error={}", 
                    promptIds, e.getMessage(), e);
            throw e; // 예외를 다시 던져서 트랜잭션 롤백
        }
    }
}

