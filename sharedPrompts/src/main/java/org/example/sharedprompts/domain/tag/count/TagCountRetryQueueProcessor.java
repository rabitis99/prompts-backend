package org.example.sharedprompts.domain.tag.count;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.tag.config.TagRedisKey;
import org.example.sharedprompts.domain.tag.event.TagCountUpdateEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

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

    /**
     * 재시도 큐 처리 (매 5초마다 실행)
     */
    @Scheduled(fixedDelayString = "${tag.count.retry.process-interval:5000}")
    public void processRetryQueue() {
        String queueKey = TagRedisKey.retryQueueKey();
        long currentTime = System.currentTimeMillis();

        try {
            // 큐에서 모든 항목 조회
            List<String> items = redisTemplate.opsForList().range(queueKey, 0, -1);
            if (items == null || items.isEmpty()) {
                return;
            }

            int processedCount = 0;
            int successCount = 0;

            for (String item : items) {
                try {
                    // 형식: <timestamp>:<json>
                    int colonIndex = item.indexOf(':');
                    if (colonIndex == -1) {
                        // 잘못된 형식, 제거
                        redisTemplate.opsForList().remove(queueKey, 1, item);
                        continue;
                    }

                    long delayTimestamp = Long.parseLong(item.substring(0, colonIndex));
                    String eventJson = item.substring(colonIndex + 1);

                    // 지연 시간이 지나지 않았으면 스킵
                    if (delayTimestamp > currentTime) {
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

                    // 성공 시 큐에서 제거
                    redisTemplate.opsForList().remove(queueKey, 1, item);
                    successCount++;
                    processedCount++;

                    log.info("Successfully reprocessed retry event: retryCount={}",
                            event.getRetryCount());

                } catch (Exception e) {
                    // 실패 로깅 강화: 원본 이벤트 JSON, stack trace, retryCount 등 상세 정보 기록
                    try {
                        int colonIndex = item.indexOf(':');
                        if (colonIndex != -1) {
                            String eventJson = item.substring(colonIndex + 1);
                            TagCountUpdateEvent event = objectMapper.readValue(eventJson, TagCountUpdateEvent.class);
                            
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
                    
                    // 실패한 항목은 큐에서 제거 (무한 루프 방지)
                    redisTemplate.opsForList().remove(queueKey, 1, item);
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

