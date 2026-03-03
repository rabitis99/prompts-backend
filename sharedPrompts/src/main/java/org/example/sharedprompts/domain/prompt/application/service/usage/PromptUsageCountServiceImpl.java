package org.example.sharedprompts.domain.prompt.application.service.usage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.shared.service.BaseCountService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 프롬프트 조회수(usageCount) 관리 서비스 구현체
 * 
 * <p>Redis 키 형식: prompt:usage:{promptId}
 * <p>Redis 장애 시 예외를 발생시키지 않고 로그만 남깁니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptUsageCountServiceImpl implements PromptUsageCountService {

    private final BaseCountService baseCountService;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List<Long>> getAndResetUsageCountsScript;

    /**
     * Redis 키 생성
     * 형식: prompt:usage:{promptId}
     */
    private String usageKey(Long promptId) {
        return "prompt:usage:" + promptId;
    }

    @Override
    public void incrementUsageCount(Long promptId) {
        try {
            baseCountService.increment(usageKey(promptId));
        } catch (Exception e) {
            // Redis 장애 시 조회 API는 실패하지 않도록 조용히 무시
            log.warn("Failed to increment usage count in Redis. promptId={}, error={}", 
                    promptId, e.getMessage());
        }
    }

    @Override
    public Map<Long, Long> getAndResetUsageCounts(List<Long> promptIds) {
        if (promptIds == null || promptIds.isEmpty()) {
            return Map.of();
        }

        try {
            // Redis 키 목록 생성
            List<String> keys = promptIds.stream()
                    .map(this::usageKey)
                    .toList();

            // Lua 스크립트 실행: 조회수 조회 및 0으로 리셋
            List<Long> values = redisTemplate.execute(
                    getAndResetUsageCountsScript,
                    keys
            );

            // 결과 맵 생성
            Map<Long, Long> result = new HashMap<>(promptIds.size());
            if (values.size() == promptIds.size()) {
                for (int i = 0; i < promptIds.size(); i++) {
                    Long promptId = promptIds.get(i);
                    long count = values.get(i) != null ? values.get(i) : 0L;
                    if (count > 0) {
                        result.put(promptId, count);
                    }
                }
            }

            log.debug("Retrieved and reset usage counts. promptIds={}, found={}", 
                    promptIds.size(), result.size());
            return result;
        } catch (Exception e) {
            log.error("Failed to get and reset usage counts from Redis. promptIds={}, error={}", 
                    promptIds, e.getMessage(), e);
            // Redis 장애 시 빈 맵 반환 (배치 처리 실패로 처리되어 다음 스케줄에서 재시도)
            return Map.of();
        }
    }

    @Override
    public void restoreUsageCounts(Map<Long, Long> usageCounts) {
        if (usageCounts == null || usageCounts.isEmpty()) {
            return;
        }

        try {
            int restoredCount = 0;
            for (Map.Entry<Long, Long> entry : usageCounts.entrySet()) {
                Long promptId = entry.getKey();
                Long count = entry.getValue();
                if (count != null && count > 0) {
                    try {
                        // Redis에 조회수 복구 (기존 값에 더하기)
                        String key = usageKey(promptId);
                        redisTemplate.opsForValue().increment(key, count);
                        restoredCount++;
                    } catch (Exception e) {
                        log.warn("Failed to restore usage count for promptId={}, count={}, error={}", 
                                promptId, count, e.getMessage());
                    }
                }
            }
            log.info("Restored usage counts to Redis. total={}, restored={}", 
                    usageCounts.size(), restoredCount);
        } catch (Exception e) {
            log.error("Failed to restore usage counts to Redis. usageCounts={}, error={}", 
                    usageCounts, e.getMessage(), e);
        }
    }
}

