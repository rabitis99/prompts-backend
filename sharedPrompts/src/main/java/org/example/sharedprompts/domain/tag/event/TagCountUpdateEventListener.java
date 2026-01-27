package org.example.sharedprompts.domain.tag.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.tag.config.TagRedisKey;
import org.example.sharedprompts.domain.tag.count.TagCountMetricService;
import org.example.sharedprompts.domain.tag.count.TagCountUpdateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 태그 카운트 업데이트 이벤트 리스너
 * - 트랜잭션 커밋 후 Redis 카운트 업데이트
 * - 큐 기반 재시도 로직으로 안정성 강화
 * - Prometheus 메트릭 수집
 * - Dead Letter Queue 지원
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagCountUpdateEventListener {

    private static final int MAX_RETRY_COUNT = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(5);

    private final TagCountUpdateService tagCountUpdateService;
    private final TagCountMetricService metricService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${tag.count.update.enable-monitoring:true}")
    private boolean enableMonitoring;

    @Value("${tag.count.update.enable-dlq:true}")
    private boolean enableDlq;

    /**
     * 태그 카운트 업데이트 이벤트 처리
     * 트랜잭션 커밋 후 비동기로 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("tagCountUpdateExecutor")
    public void handleTagCountUpdate(TagCountUpdateEvent event) {
        Timer.Sample timer = metricService.startTimer();
        
        log.debug("Processing TagCountUpdateEvent: decrease={}, increase={}, retryCount={}",
                event.getTagsToDecrease().size(), 
                event.getTagsToIncrease().size(),
                event.getRetryCount());

        try {
            // Redis 카운트 업데이트 (배치 처리)
            tagCountUpdateService.updateTagCounts(
                    event.getTagsToDecrease(),
                    event.getTagsToIncrease()
            );

            // 성공 메트릭 기록
            metricService.recordSuccess(
                    event.getTagsToDecrease().size(),
                    event.getTagsToIncrease().size()
            );
            metricService.recordDuration(timer);

            log.info("Tag count update completed successfully: decrease={}, increase={}",
                    event.getTagsToDecrease().size(),
                    event.getTagsToIncrease().size());

        } catch (Exception e) {
            log.error("Failed to update tag counts: {}", e.getMessage(), e);

            // 실패 메트릭 기록
            metricService.recordFailure(e.getClass().getSimpleName());
            metricService.recordDuration(timer);

            // TODO: 모니터링 시스템에 알림 (Sentry, NewRelic 등)
            // - Sentry 알림 연동
            // - NewRelic 메트릭 기록
            // - Slack/PagerDuty 알림 연동

            // 재시도 로직 (큐 기반)
            if (event.getRetryCount() < MAX_RETRY_COUNT) {
                scheduleRetry(event);
            } else {
                // 최대 재시도 횟수 초과 시 Dead Letter Queue에 추가
                if (enableDlq) {
                    addToDeadLetterQueue(event, e);
                }
            }
        }
    }

    /**
     * 큐 기반 재시도 스케줄링
     * CompletableFuture 재귀 구조 대신 Redis List를 사용하여 안전하게 처리
     */
    private void scheduleRetry(TagCountUpdateEvent event) {
        int newRetryCount = event.getRetryCount() + 1;
        log.warn("Scheduling retry for tag count update (attempt {}/{}): delay={}ms",
                newRetryCount, MAX_RETRY_COUNT, RETRY_DELAY.toMillis());

        try {
            // 재시도 메트릭 기록
            metricService.recordRetry(newRetryCount);

            // 지연 이벤트 큐에 추가 (Redis List 사용)
            TagCountUpdateEvent retryEvent = TagCountUpdateEvent.builder()
                    .tagsToDecrease(event.getTagsToDecrease())
                    .tagsToIncrease(event.getTagsToIncrease())
                    .retryCount(newRetryCount)
                    .occurredAt(event.getOccurredAt())
                    .build();

            String queueKey = TagRedisKey.retryQueueKey();
            String eventJson = objectMapper.writeValueAsString(retryEvent);
            
            // 지연 시간을 점수로 사용하여 정렬된 집합에 추가
            // 또는 간단하게 List에 추가하고 스케줄러가 처리
            long delayTimestamp = System.currentTimeMillis() + RETRY_DELAY.toMillis();
            String delayedEvent = delayTimestamp + ":" + eventJson;
            
            redisTemplate.opsForList().rightPush(queueKey, delayedEvent);
            
            log.debug("Retry event added to queue: retryCount={}", newRetryCount);

        } catch (Exception e) {
            log.error("Failed to schedule retry: {}", e.getMessage(), e);
            // 재시도 스케줄링 실패 시 DLQ에 추가
            if (enableDlq) {
                addToDeadLetterQueue(event, e);
            }
        }
    }

    /**
     * Dead Letter Queue에 추가
     */
    private void addToDeadLetterQueue(TagCountUpdateEvent event, Exception e) {
        try {
            String dlqKey = TagRedisKey.dlqKeyPrefix() + System.currentTimeMillis();
            
            DlqItem dlqItem = new DlqItem(
                    event.getTagsToDecrease(),
                    event.getTagsToIncrease(),
                    e.getMessage(),
                    event.getOccurredAt(),
                    event.getRetryCount(),
                    null // 초기 DLQ 추가 시에는 scheduledTime 없음
            );
            
            String dlqValue = objectMapper.writeValueAsString(dlqItem);

            // DLQ에 저장 (TTL 적용)
            redisTemplate.opsForValue().set(
                    dlqKey,
                    dlqValue,
                    TagRedisKey.dlqTtl()
            );

            // DLQ 메트릭 기록
            metricService.recordDlq();

            log.error("Tag count update event added to DLQ: key={}, retryCount={}",
                    dlqKey, event.getRetryCount());

        } catch (Exception dlqException) {
            log.error("Failed to add event to DLQ: {}", dlqException.getMessage(), dlqException);
        }
    }

    /**
     * DLQ 항목 DTO
     */
    private record DlqItem(
            Set<String> tagsToDecrease,
            Set<String> tagsToIncrease,
            String error,
            LocalDateTime occurredAt,
            int retryCount,
            Long scheduledTime  // backoff 적용 시 실행 예정 시간 (nullable)
    ) {
        // scheduledTime 없이 생성하는 편의 생성자
        public DlqItem(Set<String> tagsToDecrease, Set<String> tagsToIncrease,
                      String error, LocalDateTime occurredAt, int retryCount) {
            this(tagsToDecrease, tagsToIncrease, error, occurredAt, retryCount, null);
        }
    }
}

