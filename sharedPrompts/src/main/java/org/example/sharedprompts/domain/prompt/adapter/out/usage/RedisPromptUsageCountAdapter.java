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

/** Redis-backed implementation of prompt usage count. Failures are logged; no exceptions thrown to callers. */
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
            List<String> keys = promptIds.stream()
                    .map(RedisPromptUsageCountAdapter::usageKey)
                    .toList();

            List<Long> values = redisTemplate.execute(
                    getAndResetUsageCountsScript,
                    keys
            );

            Map<Long, Long> result = new HashMap<>(promptIds.size());
            if (values != null && values.size() == promptIds.size()) {
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
