package org.example.sharedprompts.domain.prompt.adapter.out.usage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.out.usage.PromptUsageCountPort;
import org.example.sharedprompts.domain.shared.service.BaseCountService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Redis 기반 프롬프트 사용 횟수 구현체. 실패 시 예외를 던지지 않고 로그만 기록한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPromptUsageCountAdapter implements PromptUsageCountPort {

    private final BaseCountService baseCountService;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List<Long>> getAndResetUsageCountsScript;

    private static String usageKey(Long promptId) {
        return "prompt:usage:" + promptId;
    }

    @Override
    public void incrementUsageCount(Long promptId) {
        try {
            baseCountService.increment(usageKey(promptId));
        } catch (Exception e) {
            log.warn("Redis 사용 횟수 증가 실패. promptId={}, error={}",
                    promptId, e.getMessage());
        }
    }

    @Override
    public Map<Long, Long> getAndResetUsageCounts(List<Long> promptIds) {
        if (promptIds == null || promptIds.isEmpty()) {
            return Map.of();
        }

        try {
            List<String> keys = promptIds.stream()
                    .map(RedisPromptUsageCountAdapter::usageKey)
                    .toList();

            List<Long> values = redisTemplate.execute(
                    getAndResetUsageCountsScript,
                    keys
            );

            Map<Long, Long> result = new HashMap<>(promptIds.size());
            if (values.size() == promptIds.size()) {
                for (int i = 0; i < promptIds.size(); i++) {
                    Long promptId = promptIds.get(i);
                    Long rawCount = values.get(i);
                    long count = rawCount != null ? rawCount : 0L;
                    if (count > 0) {
                        result.put(promptId, count);
                    }
                }
            }

            log.debug("사용 횟수 조회 및 초기화 완료. promptIds={}, found={}",
                    promptIds.size(), result.size());
            return result;
        } catch (Exception e) {
            log.error("Redis 사용 횟수 조회/초기화 실패. promptIds={}, error={}",
                    promptIds, e.getMessage(), e);
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
                        String key = usageKey(promptId);
                        redisTemplate.opsForValue().increment(key, count);
                        restoredCount++;
                    } catch (Exception e) {
                        log.warn("사용 횟수 복구 실패. promptId={}, count={}, error={}",
                                promptId, count, e.getMessage());
                    }
                }
            }
            log.info("Redis 사용 횟수 복구 완료. total={}, restored={}",
                    usageCounts.size(), restoredCount);
        } catch (Exception e) {
            log.error("Redis 사용 횟수 복구 실패. usageCounts={}, error={}",
                    usageCounts, e.getMessage(), e);
        }
    }
}