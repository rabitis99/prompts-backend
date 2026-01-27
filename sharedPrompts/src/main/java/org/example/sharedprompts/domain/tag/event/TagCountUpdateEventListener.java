package org.example.sharedprompts.domain.tag.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.tag.config.TagRedisKey;
import org.example.sharedprompts.domain.tag.count.DlqItem;
import org.example.sharedprompts.domain.tag.count.TagCountMetricService;
import org.example.sharedprompts.domain.tag.count.TagCountUpdateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
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

            metricService.recordFailure(e.getClass().getSimpleName());
            metricService.recordDuration(timer);

            // 재시도 로직
            if (event.getRetryCount() < MAX_RETRY_COUNT) {
                scheduleRetry(event);
            } else {
                if (enableDlq) {
                    addToDeadLetterQueue(event, e);
                } else {
                    log.warn("Max retry exceeded and DLQ disabled. Event dropped: decrease={}, increase={}",
                            event.getTagsToDecrease().size(), event.getTagsToIncrease().size());
                }
            }
        }
    }

    /**
     * 큐 기반 재시도 스케줄링
     */
    private void scheduleRetry(TagCountUpdateEvent event) {
        int newRetryCount = event.getRetryCount() + 1;
        log.warn("Scheduling retry for tag count update (attempt {}/{}): delay={}ms",
                newRetryCount, MAX_RETRY_COUNT, RETRY_DELAY.toMillis());

        try {
            metricService.recordRetry(newRetryCount);

            // 방어적 복사 적용
            TagCountUpdateEvent retryEvent = TagCountUpdateEvent.builder()
                    .tagsToDecrease(event.getTagsToDecrease())
                    .tagsToIncrease(event.getTagsToIncrease())
                    .retryCount(newRetryCount)
                    .occurredAt(event.getOccurredAt())
                    .build();

            String queueKey = TagRedisKey.retryQueueKey();
            String eventJson = objectMapper.writeValueAsString(retryEvent);

            long delayTimestamp = System.currentTimeMillis() + RETRY_DELAY.toMillis();
            String delayedEvent = delayTimestamp + ":" + eventJson;

            redisTemplate.opsForList().rightPush(queueKey, delayedEvent);

            // 큐 TTL 설정: 최초 추가 시 TTL 설정
            redisTemplate.expire(queueKey, TagRedisKey.retryQueueTtl());

            // 큐 길이 모니터링
            monitorQueueSize(queueKey);

            log.debug("Retry event added to queue: retryCount={}", newRetryCount);

        } catch (Exception e) {
            log.error("Failed to schedule retry: {}", e.getMessage(), e);
            if (enableDlq) {
                addToDeadLetterQueue(event, e);
            }
        }
    }

    /**
     * 큐 길이 모니터링 + 알림
     */
    private void monitorQueueSize(String queueKey) {
        Long queueSize = redisTemplate.opsForList().size(queueKey);
        if (queueSize != null && queueSize > 1000) { // 임계치 예시
            log.warn("Retry queue size exceeded threshold: size={}", queueSize);
            if (enableMonitoring) {
                // Slack, Sentry 등 알림 연동
            }
        }
    }

    /**
     * Dead Letter Queue에 추가
     */
    private void addToDeadLetterQueue(TagCountUpdateEvent event, Exception e) {
        try {
            String dlqKey = TagRedisKey.dlqKeyPrefix() + System.currentTimeMillis() + ":" + UUID.randomUUID();

            DlqItem dlqItem = new DlqItem(
                    event.getTagsToDecrease(),
                    event.getTagsToIncrease(),
                    e.getMessage(),
                    event.getOccurredAt(),
                    event.getRetryCount(),
                    null
            );

            String dlqValue = objectMapper.writeValueAsString(dlqItem);

            redisTemplate.opsForValue().set(
                    dlqKey,
                    dlqValue,
                    TagRedisKey.dlqTtl()
            );

            metricService.recordDlq();

            log.error("Tag count update event added to DLQ: key={}, retryCount={}",
                    dlqKey, event.getRetryCount());

        } catch (Exception dlqException) {
            log.error("Failed to add event to DLQ: {}", dlqException.getMessage(), dlqException);
        }
    }
}
