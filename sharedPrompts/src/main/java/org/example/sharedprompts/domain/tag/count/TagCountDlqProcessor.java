package org.example.sharedprompts.domain.tag.count;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.tag.config.TagRedisKey;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    
    /**
     * retryCount 기반 점진적 backoff (밀리초)
     * 1회차 → 5분, 2회차 → 15분, 3회차 → 30분, 이후 → 30분 고정
     */
    private static final long[] BACKOFF_MILLIS = {
            5 * 60_000L,   // 5분
            15 * 60_000L,  // 15분
            30 * 60_000L   // 30분
    };

    private final StringRedisTemplate redisTemplate;
    private final TagCountUpdateService tagCountUpdateService;
    private final TagCountMetricService metricService;
    private final ObjectMapper objectMapper;

    /**
     * DLQ 처리 (매 1시간마다 실행)
     */
    @Scheduled(fixedDelayString = "${tag.count.dlq.process-interval:3600000}")
    public void processDeadLetterQueue() {
        log.info("Starting DLQ processing for tag count updates");

        try {
            String dlqKeyPrefix = TagRedisKey.dlqKeyPrefix();
            Set<String> dlqKeys = redisTemplate.keys(dlqKeyPrefix + "*");
            if (dlqKeys == null || dlqKeys.isEmpty()) {
                log.debug("No items in DLQ");
                return;
            }

            int processedCount = 0;
            int successCount = 0;
            int failureCount = 0;

            for (String dlqKey : dlqKeys) {
                try {
                    String dlqValue = redisTemplate.opsForValue().get(dlqKey);
                    if (dlqValue == null) {
                        continue;
                    }

                    // JSON 파싱 및 재처리
                    DlqItem item = objectMapper.readValue(dlqValue, DlqItem.class);
                    
                    if (item.retryCount >= MAX_DLQ_RETRY) {
                        log.warn("DLQ item exceeded max retry count, removing: {} (error: {}, occurredAt: {})", 
                                dlqKey, item.error, item.occurredAt);
                        redisTemplate.delete(dlqKey);
                        continue;
                    }

                    // backoff 적용: retryCount 기반 지연 시간 계산
                    long backoffDelay = calculateBackoff(item.retryCount);
                    long executeAt = item.scheduledTime != null ? item.scheduledTime : System.currentTimeMillis();
                    
                    // 아직 실행 시간이 되지 않았으면 스킵
                    if (executeAt > System.currentTimeMillis()) {
                        log.debug("DLQ item not ready yet: key={}, executeAt={}, currentTime={}", 
                                dlqKey, executeAt, System.currentTimeMillis());
                        continue;
                    }

                    log.debug("Processing DLQ item: key={}, error={}, occurredAt={}, retryCount={}, backoffDelay={}ms", 
                            dlqKey, item.error, item.occurredAt, item.retryCount, backoffDelay);

                    // 재처리 시도
                    tagCountUpdateService.updateTagCounts(
                            item.tagsToDecrease,
                            item.tagsToIncrease
                    );

                    // 성공 시 DLQ에서 제거
                    redisTemplate.delete(dlqKey);
                    successCount++;
                    metricService.recordSuccess(
                            item.tagsToDecrease.size(),
                            item.tagsToIncrease.size()
                    );
                    log.info("Successfully reprocessed DLQ item: {}", dlqKey);

                } catch (Exception e) {
                    log.error("Failed to process DLQ item {}: {}", dlqKey, e.getMessage(), e);
                    
                    // 실패 시 backoff 적용하여 재등록
                    try {
                        DlqItem item = objectMapper.readValue(
                                redisTemplate.opsForValue().get(dlqKey), 
                                DlqItem.class
                        );
                        
                        if (item != null && item.retryCount < MAX_DLQ_RETRY) {
                            int newRetryCount = item.retryCount + 1;
                            long backoffDelay = calculateBackoff(newRetryCount);
                            long executeAt = System.currentTimeMillis() + backoffDelay;
                            
                            DlqItem updatedItem = new DlqItem(
                                    item.tagsToDecrease,
                                    item.tagsToIncrease,
                                    item.error,
                                    item.occurredAt,
                                    newRetryCount,
                                    executeAt
                            );
                            
                            String updatedValue = objectMapper.writeValueAsString(updatedItem);
                            redisTemplate.opsForValue().set(
                                    dlqKey,
                                    updatedValue,
                                    TagRedisKey.dlqTtl()
                            );
                            
                            log.warn("DLQ item rescheduled with backoff: key={}, retryCount={}, executeAt={} (delay={}ms)", 
                                    dlqKey, newRetryCount, executeAt, backoffDelay);
                        }
                    } catch (Exception rescheduleException) {
                        log.error("Failed to reschedule DLQ item {}: {}", dlqKey, rescheduleException.getMessage(), rescheduleException);
                    }
                    
                    failureCount++;
                }

                processedCount++;
            }

            log.info("DLQ processing completed: processed={}, success={}, failure={}",
                    processedCount, successCount, failureCount);

        } catch (Exception e) {
            log.error("Error during DLQ processing: {}", e.getMessage(), e);
        }
    }

    /**
     * retryCount 기반 backoff 시간 계산
     * 
     * @param retryCount 현재 재시도 횟수
     * @return backoff 지연 시간 (밀리초)
     */
    private long calculateBackoff(int retryCount) {
        if (retryCount <= 0) {
            return BACKOFF_MILLIS[0];
        }
        
        int index = retryCount - 1;
        if (index < BACKOFF_MILLIS.length) {
            return BACKOFF_MILLIS[index];
        }
        
        // 최대 backoff 시간 반환
        return BACKOFF_MILLIS[BACKOFF_MILLIS.length - 1];
    }

    /**
     * DLQ 항목 DTO
     */
    private static class DlqItem {
        public Set<String> tagsToDecrease;
        public Set<String> tagsToIncrease;
        public String error;
        public String occurredAt;
        public int retryCount = 0;
        public Long scheduledTime; // backoff 적용 시 실행 예정 시간
        
        // 기본 생성자 (Jackson용)
        public DlqItem() {}
        
        // 전체 생성자
        public DlqItem(Set<String> tagsToDecrease, Set<String> tagsToIncrease, 
                      String error, String occurredAt, int retryCount, Long scheduledTime) {
            this.tagsToDecrease = tagsToDecrease;
            this.tagsToIncrease = tagsToIncrease;
            this.error = error;
            this.occurredAt = occurredAt;
            this.retryCount = retryCount;
            this.scheduledTime = scheduledTime;
        }
    }
}

