package org.example.sharedprompts.scheduler.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.service.usage.PromptUsageCountService;
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
 * <p>DB 반영 실패 시 Redis 값은 복구하여 다음 스케줄에서 재시도할 수 있도록 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptUsageCountBatchService {

    private final PromptUsageCountService promptUsageCountService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 프롬프트 ID 목록에 대한 조회수를 배치로 처리
     * 
     * <p>1. Redis에서 누적된 조회수를 원자적으로 조회하고 0으로 리셋 (Lua 스크립트)
     * <p>2. DB의 view_count에 batch로 합산
     * <p>3. 반영 성공 시 완료
     * <p>4. 반영 실패 시 Redis에 조회수 복구 (다음 스케줄에서 재시도)
     * 
     * @param promptIds 처리할 프롬프트 ID 목록
     */
    @Transactional
    protected void processBatch(List<Long> promptIds) {
        if (promptIds == null || promptIds.isEmpty()) {
            return;
        }

        // 1. Redis에서 누적된 조회수를 원자적으로 조회하고 0으로 리셋
        Map<Long, Long> usageCounts = promptUsageCountService.getAndResetUsageCounts(promptIds);

        // 2. 조회수가 있는 프롬프트만 필터링하여 배치 업데이트 준비
        List<Object[]> batchArgs = promptIds.stream()
                .filter(usageCounts::containsKey)
                .filter(id -> usageCounts.get(id) > 0) // 0보다 큰 값만 처리
                .map(id -> new Object[]{usageCounts.get(id), id})
                .collect(Collectors.toList());

        if (batchArgs.isEmpty()) {
            log.debug("No usage counts to process. promptIds={}", promptIds.size());
            return;
        }

        try {
            // 3. DB의 view_count에 batch로 합산
            String sql = "UPDATE prompts SET view_count = view_count + ? WHERE id = ?";
            jdbcTemplate.batchUpdate(sql, batchArgs);

            log.debug("Processed usage count batch: requested={}, updated={}", promptIds.size(), batchArgs.size());

        } catch (Exception e) {
            // 4. DB 반영 실패 시 Redis에 조회수 복구 (다음 스케줄에서 재시도 가능)
            log.error("Failed to process usage count batch. Restoring Redis values. promptIds={}, error={}", 
                    promptIds, e.getMessage(), e);
            
            try {
                promptUsageCountService.restoreUsageCounts(usageCounts);
                log.info("Restored usage counts to Redis after DB failure. count={}", usageCounts.size());
            } catch (Exception restoreException) {
                log.error("Failed to restore usage counts to Redis. This may cause data loss. usageCounts={}, error={}", 
                        usageCounts, restoreException.getMessage(), restoreException);
            }
            
            throw e; // 예외를 다시 던져서 트랜잭션 롤백
        }
    }
}

