package org.example.sharedprompts.domain.tag.count;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.tag.config.TagRedisKey;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Dead Letter Queue 처리 스케줄러
 * - 주기적으로 DLQ를 확인하여 재처리 시도
 * - retryCount 기반 점진적 backoff 적용
 * - 수동 복구도 가능하도록 구성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagCountDlqProcessor {

    private static final int MAX_DLQ_RETRY = 5;

    private static final long[] BACKOFF_MILLIS = {
            5 * 60_000L,    // retryCount 1: 5분
            15 * 60_000L,   // retryCount 2: 15분
            30 * 60_000L,   // retryCount 3: 30분
            60 * 60_000L,   // retryCount 4: 1시간
            120 * 60_000L   // retryCount 5: 2시간
    };

    private final StringRedisTemplate redisTemplate;
    private final TagCountUpdateService tagCountUpdateService;
    private final TagCountMetricService metricService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${tag.count.dlq.process-interval:3600000}")
    public void processDeadLetterQueue() {
        log.info("Starting DLQ processing for tag count updates");

        try {
            Set<String> dlqKeys = scanDlqKeys(TagRedisKey.dlqKeyPrefix());
            if (dlqKeys.isEmpty()) {
                log.debug("No items in DLQ");
                return;
            }

            int processedCount = 0;
            int successCount = 0;
            int failureCount = 0;

            for (String dlqKey : dlqKeys) {
                DlqItem item = null;
                try {
                    String dlqValue = redisTemplate.opsForValue().get(dlqKey);
                    if (dlqValue == null) continue;

                    item = objectMapper.readValue(dlqValue, DlqItem.class);

                    if (item.getRetryCount() >= MAX_DLQ_RETRY) {
                        log.warn("DLQ item exceeded max retry count, removing: {} (error: {}, occurredAt: {})",
                                dlqKey, item.getError(), item.getOccurredAt());
                        redisTemplate.delete(dlqKey);
                        continue;
                    }

                    long executeAt = item.getScheduledTimeMillis() != null ? item.getScheduledTimeMillis() : System.currentTimeMillis();
                    if (executeAt > System.currentTimeMillis()) {
                        log.debug("DLQ item not ready yet: key={}, executeAt={}, currentTime={}",
                                dlqKey, executeAt, System.currentTimeMillis());
                        continue;
                    }

                    tagCountUpdateService.updateTagCounts(item.getTagsToDecrease(), item.getTagsToIncrease());

                    redisTemplate.delete(dlqKey);
                    successCount++;
                    metricService.recordSuccess(item.getTagsToDecrease().size(), item.getTagsToIncrease().size());
                    log.info("Successfully reprocessed DLQ item: {}", dlqKey);

                } catch (Exception e) {
                    log.error("Failed to process DLQ item {}: {}", dlqKey, e.getMessage(), e);
                    metricService.recordFailure(e.getClass().getSimpleName());
                    // 이미 파싱된 item을 재사용하여 데이터 불일치 방지
                    if (item != null) {
                        rescheduleDlqItem(dlqKey, item);
                    } else {
                        // 파싱 실패 시에만 Redis에서 재조회
                        rescheduleDlqItem(dlqKey, null);
                    }
                    failureCount++;
                }

                processedCount++;
            }

            log.info("DLQ processing completed: processed={}, success={}, failure={}", processedCount, successCount, failureCount);

        } catch (Exception e) {
            log.error("Error during DLQ processing: {}", e.getMessage(), e);
        }
    }

    /**
     * retryCount 기반 backoff 시간 계산
     */
    private long calculateBackoff(int retryCount) {
        if (retryCount <= 0) return BACKOFF_MILLIS[0];
        int index = retryCount - 1;
        return index < BACKOFF_MILLIS.length ? BACKOFF_MILLIS[index] : BACKOFF_MILLIS[BACKOFF_MILLIS.length - 1];
    }

    /**
     * SCAN을 이용하여 DLQ 키 조회
     */
    private Set<String> scanDlqKeys(String prefix) {
        Set<String> dlqKeys = new HashSet<>();
        ScanOptions scanOptions = ScanOptions.scanOptions().match(prefix + "*").count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                dlqKeys.add(cursor.next());
            }
        } catch (Exception e) {
            log.error("Error scanning DLQ keys with prefix {}: {}", prefix, e.getMessage(), e);
        }
        return dlqKeys;
    }

    /**
     * 실패한 DLQ 아이템 재등록
     * 
     * @param dlqKey DLQ 키
     * @param item 이미 파싱된 아이템 (null이면 Redis에서 재조회)
     */
    private void rescheduleDlqItem(String dlqKey, DlqItem item) {
        try {
            // 이미 파싱된 item이 없으면 Redis에서 재조회 (파싱 실패 케이스)
            if (item == null) {
                String dlqValue = redisTemplate.opsForValue().get(dlqKey);
                if (dlqValue == null) return;
                item = objectMapper.readValue(dlqValue, DlqItem.class);
            }
            
            if (item.getRetryCount() >= MAX_DLQ_RETRY) return;

            int newRetryCount = item.getRetryCount() + 1;
            long backoffDelay = calculateBackoff(newRetryCount);
            long executeAt = System.currentTimeMillis() + backoffDelay;

            DlqItem updatedItem = new DlqItem(
                    item.getTagsToDecrease(),
                    item.getTagsToIncrease(),
                    item.getError(),
                    item.getOccurredAt(),
                    newRetryCount,
                    executeAt
            );

            String updatedValue = objectMapper.writeValueAsString(updatedItem);
            redisTemplate.opsForValue().set(dlqKey, updatedValue, TagRedisKey.dlqTtl());

            log.warn("DLQ item rescheduled with backoff: key={}, retryCount={}, executeAt={} (delay={}ms)",
                    dlqKey, newRetryCount, executeAt, backoffDelay);
        } catch (Exception e) {
            log.error("Failed to reschedule DLQ item {}: {}", dlqKey, e.getMessage(), e);
        }
    }
}
