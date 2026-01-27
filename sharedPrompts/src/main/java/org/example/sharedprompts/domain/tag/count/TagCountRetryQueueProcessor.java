package org.example.sharedprompts.domain.tag.count;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.tag.config.TagRedisKey;
import org.example.sharedprompts.domain.tag.event.TagCountUpdateEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 지연 이벤트 큐 처리 스케줄러
 * - 주기적으로 재시도 큐를 확인하여 처리
 * - 큐 기반 재시도로 CompletableFuture 재귀 구조 대체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagCountRetryQueueProcessor {

    private final StringRedisTemplate redisTemplate;
    private final TagCountUpdateService tagCountUpdateService;
    private final TagCountMetricService metricService;
    private final ObjectMapper objectMapper;

    @Value("${tag.count.update.enable-dlq:true}")
    private boolean enableDlq;

    /**
     * 재시도 큐 처리 (매 5초마다 실행)
     * 
     * 다중 인스턴스 환경에서 경쟁 조건을 방지하기 위해 LPOP을 사용하여 원자적으로 처리합니다.
     */
    @Scheduled(fixedDelayString = "${tag.count.retry.process-interval:5000}")
    public void processRetryQueue() {
        String queueKey = TagRedisKey.retryQueueKey();
        long currentTime = System.currentTimeMillis();

        try {
            int processedCount = 0;
            int successCount = 0;
            int maxItemsPerIteration = 100; // 한 번에 처리할 최대 항목 수 (무한 루프 방지)

            // LPOP을 사용하여 원자적으로 항목을 가져와 처리
            // 다중 인스턴스 환경에서 동일한 항목이 중복 처리되는 것을 방지
            for (int i = 0; i < maxItemsPerIteration; i++) {
                String item = redisTemplate.opsForList().leftPop(queueKey);
                if (item == null) {
                    // 큐가 비어있음
                    break;
                }

                try {
                    // 형식: <timestamp>:<json>
                    int colonIndex = item.indexOf(':');
                    if (colonIndex == -1) {
                        // 잘못된 형식, 이미 LPOP으로 제거되었으므로 로그만 기록
                        log.warn("Invalid retry queue item format, skipped: {}", item);
                        continue;
                    }

                    long delayTimestamp = Long.parseLong(item.substring(0, colonIndex));
                    String eventJson = item.substring(colonIndex + 1);

                    // 지연 시간이 지나지 않았으면 큐의 오른쪽 끝에 다시 추가 (RPUSH)
                    if (delayTimestamp > currentTime) {
                        redisTemplate.opsForList().rightPush(queueKey, item);
                        continue;
                    }

                    // 이벤트 파싱 및 재처리
                    TagCountUpdateEvent event = objectMapper.readValue(
                            eventJson, 
                            TagCountUpdateEvent.class
                    );

                    // 재처리 시도
                    tagCountUpdateService.updateTagCounts(
                            event.getTagsToDecrease(),
                            event.getTagsToIncrease()
                    );

                    // 성공 메트릭 기록
                    metricService.recordSuccess(
                            event.getTagsToDecrease().size(),
                            event.getTagsToIncrease().size()
                    );

                    // 성공 시 이미 LPOP으로 제거되었으므로 추가 작업 불필요
                    successCount++;
                    processedCount++;

                    log.info("Successfully reprocessed retry event: retryCount={}",
                            event.getRetryCount());

                } catch (Exception e) {
                    // 실패 로깅 강화: 원본 이벤트 JSON, stack trace, retryCount 등 상세 정보 기록
                    TagCountUpdateEvent event = null;
                    try {
                        int colonIndex = item.indexOf(':');
                        if (colonIndex != -1) {
                            String eventJson = item.substring(colonIndex + 1);
                            event = objectMapper.readValue(eventJson, TagCountUpdateEvent.class);
                            
                            log.error("Retry event failed: retryCount={}, scheduledTime={}, event={}, exception={}", 
                                    event.getRetryCount(),
                                    colonIndex > 0 ? item.substring(0, colonIndex) : "unknown",
                                    eventJson,
                                    getStackTrace(e));
                        } else {
                            log.error("Retry event failed (invalid format): item={}, exception={}", 
                                    item,
                                    getStackTrace(e));
                        }
                    } catch (Exception logException) {
                        log.error("Failed to parse retry event for logging: item={}, exception={}", 
                                item,
                                getStackTrace(e));
                    }
                    
                    // 실패 메트릭 기록
                    metricService.recordFailure(e.getClass().getSimpleName());
                    
                    // 실패한 항목은 DLQ에 추가하여 이벤트 손실 방지
                    if (enableDlq && event != null) {
                        addToDeadLetterQueue(event, e);
                    }
                    
                    // 실패한 항목은 이미 LPOP으로 제거되었으므로 추가 작업 불필요
                    // 무한 루프 방지를 위해 재추가하지 않음
                    processedCount++;
                }
            }

            if (processedCount > 0) {
                log.debug("Retry queue processing completed: processed={}, success={}",
                        processedCount, successCount);
            }

        } catch (Exception e) {
            log.error("Error during retry queue processing: {}", e.getMessage(), e);
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

            log.error("Retry queue processing failed, event added to DLQ: key={}, retryCount={}",
                    dlqKey, event.getRetryCount());

        } catch (Exception dlqException) {
            log.error("Failed to add event to DLQ: {}", dlqException.getMessage(), dlqException);
        }
    }

    /**
     * 예외의 stack trace를 문자열로 변환
     * 
     * @param e 예외
     * @return stack trace 문자열
     */
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}

