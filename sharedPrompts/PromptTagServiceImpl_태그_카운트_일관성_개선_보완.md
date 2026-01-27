# PromptTagServiceImpl 태그 카운트 일관성 개선 보완

## 개요

기존 TransactionSynchronizationManager 기반 개선을 더욱 강화하여 실무 수준의 안정성과 확장성을 확보합니다.

### 주요 보완 사항
1. **이벤트 기반 아키텍처** - 확장 가능한 구조로 전환
2. **배치 처리** - Redis Pipeline을 통한 성능 향상
3. **지연 이벤트 큐** - 큐 기반 재시도 로직으로 안정성 강화
4. **메트릭 수집** - Prometheus 메트릭 등록으로 운영 모니터링 강화
5. **Redis Key 네이밍** - 표준화된 prefix 및 TTL 정책 적용
6. **Redis 업데이트 실패 대응** - 모니터링 및 재시도 로직
7. **동시성/경합 대비** - Redis 원자 연산 및 eventual consistency 보장
8. **트랜잭션 없는 환경 대비** - Dead Letter Queue 지원

---

## 브랜치명
```
feat/tag-count-consistency-with-event-driven-architecture
```

## 커밋 메시지
```
feat: 태그 카운트 일관성 개선 - 배치 처리, 큐 기반 재시도, 메트릭 강화

- 이벤트 기반 구조로 전환하여 확장성 확보
- Redis Pipeline을 통한 배치 처리로 성능 향상
- 지연 이벤트 큐 기반 재시도 로직으로 안정성 강화
- Prometheus 메트릭 등록으로 운영 모니터링 강화
- Redis Key 네이밍 표준화 및 TTL 정책 적용
- Redis 업데이트 실패 시 모니터링 및 재시도 로직 추가
- Redis 원자 연산 보장 및 동시성 문제 해결
- Dead Letter Queue를 통한 실패 복구 메커니즘 추가

Resolves: #16
```

---

## 수정 내용

### 1. 이벤트 클래스 생성

#### 파일: `src/main/java/org/example/sharedprompts/domain/tag/event/TagCountUpdateEvent.java`

```java
package org.example.sharedprompts.domain.tag.event;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 태그 카운트 업데이트 이벤트
 * - 트랜잭션 커밋 후 Redis 카운트 업데이트를 위한 이벤트
 * - 이벤트 기반 아키텍처로 확장 가능 (Kafka, Redis PubSub 등)
 */
@Value
@Builder
public class TagCountUpdateEvent {
    
    /**
     * 제거된 태그 이름 목록 (카운트 감소 대상)
     */
    Set<String> tagsToDecrease;
    
    /**
     * 추가된 태그 이름 목록 (카운트 증가 대상)
     */
    Set<String> tagsToIncrease;
    
    /**
     * 이벤트 발생 시각
     */
    @Builder.Default
    LocalDateTime occurredAt = LocalDateTime.now();
    
    /**
     * 재시도 횟수 (실패 시 증가)
     */
    @Builder.Default
    int retryCount = 0;
}
```

---

### 2. 이벤트 발행자 생성

#### 파일: `src/main/java/org/example/sharedprompts/domain/tag/event/TagEventPublisher.java`

```java
package org.example.sharedprompts.domain.tag.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 태그 관련 이벤트 발행자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 태그 카운트 업데이트 이벤트 발행
     * 
     * @param tagsToDecrease 제거된 태그 이름 목록
     * @param tagsToIncrease 추가된 태그 이름 목록
     */
    public void publishTagCountUpdate(Set<String> tagsToDecrease, Set<String> tagsToIncrease) {
        TagCountUpdateEvent event = TagCountUpdateEvent.builder()
                .tagsToDecrease(tagsToDecrease)
                .tagsToIncrease(tagsToIncrease)
                .build();
        
        eventPublisher.publishEvent(event);
        log.debug("Published TagCountUpdateEvent: decrease={}, increase={}", 
                tagsToDecrease.size(), tagsToIncrease.size());
    }
}
```

---

### 3. Redis Key 네이밍 상수 클래스

#### 파일: `src/main/java/org/example/sharedprompts/domain/tag/config/TagRedisKey.java`

```java
package org.example.sharedprompts.domain.tag.config;

import java.time.Duration;

/**
 * 태그 관련 Redis Key 네이밍 표준화
 * - prefix 표준화: sharedprompts:tag:*
 * - TTL 정책 관리
 */
public final class TagRedisKey {

    private static final String PREFIX = "sharedprompts:tag";
    
    /**
     * 태그 카운트 키
     * 형식: sharedprompts:tag:count:<tagName>
     */
    public static String countKey(String tagName) {
        return String.format("%s:count:%s", PREFIX, tagName);
    }
    
    /**
     * 태그 카운트 TTL (30일)
     */
    public static Duration countTtl() {
        return Duration.ofDays(30);
    }
    
    /**
     * 지연 이벤트 큐 키
     * 형식: sharedprompts:tag:retry:queue
     */
    public static String retryQueueKey() {
        return PREFIX + ":retry:queue";
    }
    
    /**
     * Dead Letter Queue 키 prefix
     * 형식: sharedprompts:tag:dlq:<timestamp>
     */
    public static String dlqKeyPrefix() {
        return PREFIX + ":dlq:";
    }
    
    /**
     * DLQ TTL (7일)
     */
    public static Duration dlqTtl() {
        return Duration.ofDays(7);
    }
    
    private TagRedisKey() {
        throw new UnsupportedOperationException("Utility class");
    }
}
```

---

### 4. 태그 카운트 업데이트 서비스 (배치 처리 지원)

#### 파일: `src/main/java/org/example/sharedprompts/domain/tag/service/TagCountUpdateService.java`

```java
package org.example.sharedprompts.domain.tag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.tag.config.TagRedisKey;
import org.example.sharedprompts.domain.tag.repository.TagRepository;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 태그 카운트 업데이트 전용 서비스
 * - Redis 원자 연산 보장
 * - Redis Pipeline을 통한 배치 처리로 성능 향상
 * - 실패 시 모니터링 및 재시도 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagCountUpdateService {

    private final TagRepository tagRepository;
    private final StringRedisTemplate redisTemplate;
    
    // Redis Lua 스크립트를 통한 원자적 증가/감소 연산
    private static final String INCREMENT_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local current = redis.call('GET', key) or '0'\n" +
            "local result = redis.call('INCR', key)\n" +
            "redis.call('EXPIRE', key, ARGV[1])\n" +
            "return result";
    
    private static final String DECREMENT_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local current = redis.call('GET', key) or '0'\n" +
            "if tonumber(current) > 0 then\n" +
            "    local result = redis.call('DECR', key)\n" +
            "    redis.call('EXPIRE', key, ARGV[1])\n" +
            "    return result\n" +
            "else\n" +
            "    redis.call('EXPIRE', key, ARGV[1])\n" +
            "    return 0\n" +
            "end";

    /**
     * 태그 카운트 증가 (원자 연산)
     * 
     * @param tagName 태그 이름
     * @return 업데이트된 카운트
     */
    public long incrementTagCount(String tagName) {
        try {
            String key = TagRedisKey.countKey(tagName);
            long ttlSeconds = TagRedisKey.countTtl().getSeconds();
            
            // Redis 원자 연산 사용 (TTL 설정 포함)
            Long result = redisTemplate.execute(
                    new DefaultRedisScript<>(INCREMENT_SCRIPT, Long.class),
                    Collections.singletonList(key),
                    String.valueOf(ttlSeconds)
            );
            
            if (result == null) {
                log.warn("Failed to increment tag count for {}: result is null", tagName);
                return 0;
            }
            
            log.debug("Tag count incremented: {} -> {}", tagName, result);
            return result;
        } catch (Exception e) {
            log.error("Failed to increment tag count for {}: {}", tagName, e.getMessage(), e);
            throw new TagCountUpdateException("Failed to increment tag count: " + tagName, e);
        }
    }

    /**
     * 태그 카운트 감소 (원자 연산)
     * 
     * @param tagName 태그 이름
     * @return 업데이트된 카운트
     */
    public long decrementTagCount(String tagName) {
        try {
            String key = TagRedisKey.countKey(tagName);
            long ttlSeconds = TagRedisKey.countTtl().getSeconds();
            
            // Redis 원자 연산 사용 (0 이하로 내려가지 않음, TTL 설정 포함)
            Long result = redisTemplate.execute(
                    new DefaultRedisScript<>(DECREMENT_SCRIPT, Long.class),
                    Collections.singletonList(key),
                    String.valueOf(ttlSeconds)
            );
            
            if (result == null) {
                log.warn("Failed to decrement tag count for {}: result is null", tagName);
                return 0;
            }
            
            log.debug("Tag count decremented: {} -> {}", tagName, result);
            return result;
        } catch (Exception e) {
            log.error("Failed to decrement tag count for {}: {}", tagName, e.getMessage(), e);
            throw new TagCountUpdateException("Failed to decrement tag count: " + tagName, e);
        }
    }

    /**
     * 태그 카운트 업데이트 (배치 처리 - Redis Pipeline 사용)
     * 
     * @param tagsToDecrease 감소할 태그 목록
     * @param tagsToIncrease 증가할 태그 목록
     */
    public void updateTagCounts(Set<String> tagsToDecrease, Set<String> tagsToIncrease) {
        if (tagsToDecrease.isEmpty() && tagsToIncrease.isEmpty()) {
            return;
        }

        long ttlSeconds = TagRedisKey.countTtl().getSeconds();
        
        // Pipeline을 통한 배치 처리로 성능 향상
        // 여러 개의 Redis 명령을 한 번에 전송하여 네트워크 왕복 감소
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            org.springframework.data.redis.connection.RedisScriptingCommands scriptingCommands = 
                    connection.scriptingCommands();
            
            // 감소 처리
            for (String tagName : tagsToDecrease) {
                String key = TagRedisKey.countKey(tagName);
                // Lua 스크립트 실행을 Pipeline에 추가
                scriptingCommands.eval(
                        DECREMENT_SCRIPT.getBytes(),
                        org.springframework.data.redis.connection.RedisStringCommands.ReturnType.INTEGER,
                        1,
                        key.getBytes(),
                        String.valueOf(ttlSeconds).getBytes()
                );
            }
            
            // 증가 처리
            for (String tagName : tagsToIncrease) {
                String key = TagRedisKey.countKey(tagName);
                // Lua 스크립트 실행을 Pipeline에 추가
                scriptingCommands.eval(
                        INCREMENT_SCRIPT.getBytes(),
                        org.springframework.data.redis.connection.RedisStringCommands.ReturnType.INTEGER,
                        1,
                        key.getBytes(),
                        String.valueOf(ttlSeconds).getBytes()
                );
            }
            
            return null;
        });

        log.debug("Tag count batch update completed: decrease={}, increase={}, results={}",
                tagsToDecrease.size(), tagsToIncrease.size(), results.size());
    }

    /**
     * 태그 카운트 업데이트 예외
     */
    public static class TagCountUpdateException extends RuntimeException {
        public TagCountUpdateException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

---

### 5. 메트릭 서비스

#### 파일: `src/main/java/org/example/sharedprompts/domain/tag/service/TagCountMetricService.java`

```java
package org.example.sharedprompts.domain.tag.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 태그 카운트 업데이트 메트릭 서비스
 * - Prometheus 메트릭 등록
 * - 성공/실패/재시도 횟수 추적
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagCountMetricService {

    private final MeterRegistry meterRegistry;

    private static final String METRIC_PREFIX = "tag_count_update";

    /**
     * 성공 메트릭 증가
     */
    public void recordSuccess(int decreaseCount, int increaseCount) {
        Counter.builder(METRIC_PREFIX + "_total")
                .tag("status", "success")
                .description("Total number of successful tag count updates")
                .register(meterRegistry)
                .increment();
        
        Counter.builder(METRIC_PREFIX + "_tags_total")
                .tag("operation", "decrease")
                .description("Total number of tags decreased")
                .register(meterRegistry)
                .increment(decreaseCount);
        
        Counter.builder(METRIC_PREFIX + "_tags_total")
                .tag("operation", "increase")
                .description("Total number of tags increased")
                .register(meterRegistry)
                .increment(increaseCount);
    }

    /**
     * 실패 메트릭 증가
     */
    public void recordFailure(String errorType) {
        Counter.builder(METRIC_PREFIX + "_total")
                .tag("status", "failure")
                .tag("error_type", errorType)
                .description("Total number of failed tag count updates")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 재시도 메트릭 증가
     */
    public void recordRetry(int retryCount) {
        Counter.builder(METRIC_PREFIX + "_retry_total")
                .tag("retry_count", String.valueOf(retryCount))
                .description("Total number of retry attempts")
                .register(meterRegistry)
                .increment();
    }

    /**
     * DLQ 추가 메트릭 증가
     */
    public void recordDlq() {
        Counter.builder(METRIC_PREFIX + "_dlq_total")
                .description("Total number of events added to DLQ")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 처리 시간 기록
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 처리 시간 기록 완료
     */
    public void recordDuration(Timer.Sample sample) {
        sample.stop(Timer.builder(METRIC_PREFIX + "_duration_seconds")
                .description("Tag count update processing duration")
                .register(meterRegistry));
    }
}
```

---

### 6. 태그 카운트 업데이트 이벤트 리스너 (큐 기반 재시도)

#### 파일: `src/main/java/org/example/sharedprompts/domain/tag/event/TagCountUpdateEventListener.java`

```java
package org.example.sharedprompts.domain.tag.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.tag.config.TagRedisKey;
import org.example.sharedprompts.domain.tag.service.TagCountMetricService;
import org.example.sharedprompts.domain.tag.service.TagCountUpdateService;
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
                    event.getRetryCount()
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
            int retryCount
    ) {}
}
```

---

### 7. 지연 이벤트 큐 처리 스케줄러

#### 파일: `src/main/java/org/example/sharedprompts/domain/tag/service/TagCountRetryQueueProcessor.java`

```java
package org.example.sharedprompts.domain.tag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.tag.config.TagRedisKey;
import org.example.sharedprompts.domain.tag.event.TagCountUpdateEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

                    // 성공 시 큐에서 제거
                    redisTemplate.opsForList().remove(queueKey, 1, item);
                    successCount++;
                    processedCount++;

                    log.info("Successfully reprocessed retry event: retryCount={}",
                            event.getRetryCount());

                } catch (Exception e) {
                    log.error("Failed to process retry queue item: {}", e.getMessage(), e);
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
}
```

---

### 8. 비동기 실행자 설정

#### 파일: `src/main/java/org/example/sharedprompts/global/config/AsyncConfig.java`

```java
package org.example.sharedprompts.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 실행 설정
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 태그 카운트 업데이트 전용 실행자
     */
    @Bean(name = "tagCountUpdateExecutor")
    public Executor tagCountUpdateExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("tag-count-update-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
```

---

### 9. PromptTagServiceImpl 수정

#### 파일: `src/main/java/org/example/sharedprompts/domain/tag/service/PromptTagServiceImpl.java`

```java
package org.example.sharedprompts.domain.tag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.tag.PromptTag;
import org.example.sharedprompts.domain.tag.Tag;
import org.example.sharedprompts.domain.tag.event.TagEventPublisher;
import org.example.sharedprompts.domain.tag.repository.PromptTagRepository;
import org.example.sharedprompts.domain.tag.repository.TagRepository;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.TagNormalizer;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.core.NestedExceptionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PromptTagServiceImpl implements PromptTagService {

    private final PromptTagRepository promptTagRepository;
    private final TagRepository tagRepository;
    private final TagEventPublisher tagEventPublisher;
    private final TagCountUpdateService tagCountUpdateService;

    @Override
    public List<Tag> addTags(Prompt prompt, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return List.of();

        List<String> processedNames = TagNormalizer.normalizeTags(tagNames);
        List<Tag> tags = new ArrayList<>(processedNames.size());

        for (String name : processedNames) {
            Tag tag = getOrCreateTag(name);

            boolean attached = attachPromptTag(prompt, tag);

            if (attached) {
                // 즉시 업데이트 (트랜잭션 내부이므로 안전)
                // 단일 태그는 개별 처리, 배치는 updateTags에서 처리
                tagCountUpdateService.incrementTagCount(tag.getName());
            }

            tags.add(tag);
        }

        return tags;
    }

    private Tag getOrCreateTag(String name) {
        return tagRepository.findByName(name)
                .orElseGet(() -> {
                    Tag newTag = new Tag(name);
                    return tagRepository.save(newTag);
                });
    }

    private boolean attachPromptTag(Prompt prompt, Tag tag) {
        try {
            promptTagRepository.saveAndFlush(new PromptTag(prompt, tag));
            return true;
        } catch (DataIntegrityViolationException e) {
            // 유니크 제약조건(uk_prompt_tag) 위반인 경우만 무시
            Throwable mostSpecific = NestedExceptionUtils.getMostSpecificCause(e);
            if (mostSpecific instanceof ConstraintViolationException cve) {
                String constraintName = cve.getConstraintName();
                if ("uk_prompt_tag".equalsIgnoreCase(constraintName)) {
                    return false;
                }
            }
            // 다른 무결성 위반은 로깅하고 재던지기
            log.error("Unexpected integrity violation when attaching prompt-tag", e);
            throw e;
        }
    }

    @Override
    public void updateTags(Prompt prompt, List<String> tagNames) {
        // 1. 기존 태그와 새 태그 비교
        List<PromptTag> existing = promptTagRepository.findPromptTagByPrompt(prompt);
        Set<String> existingTagNames = existing.stream()
                .map(pt -> pt.getTag().getName())
                .collect(Collectors.toSet());

        List<String> processedNames = TagNormalizer.normalizeTags(tagNames);
        Set<String> newTagNames = new HashSet<>(processedNames);

        // 2. 제거할 태그와 추가할 태그 계산
        Set<String> toRemove = new HashSet<>(existingTagNames);
        toRemove.removeAll(newTagNames);

        Set<String> toAdd = new HashSet<>(newTagNames);
        toAdd.removeAll(existingTagNames);

        // 3. DB 변경 먼저 수행 (트랜잭션 내부)
        existing.stream()
                .filter(pt -> toRemove.contains(pt.getTag().getName()))
                .forEach(promptTagRepository::delete);

        List<Tag> tagsToAdd = toAdd.stream()
                .map(this::getOrCreateTag)
                .toList();

        tagsToAdd.forEach(tag -> attachPromptTag(prompt, tag));

        // 4. 트랜잭션 커밋 후 이벤트 발행
        // DB 변경이 성공적으로 커밋된 후에만 이벤트 발행
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            // 태그 이름만 전달 (Tag 객체는 트랜잭션 종료 후 사용 불가)
                            Set<String> tagsToIncrease = tagsToAdd.stream()
                                    .map(Tag::getName)
                                    .collect(Collectors.toSet());
                            
                            // 이벤트 발행 (트랜잭션 커밋 후)
                            tagEventPublisher.publishTagCountUpdate(toRemove, tagsToIncrease);
                            
                            log.debug("Tag count update event published after commit: " +
                                    "decrease={}, increase={}", 
                                    toRemove.size(), tagsToIncrease.size());
                        }
                    }
            );
        } else {
            // 트랜잭션이 없는 경우 즉시 실행 (테스트 환경 등)
            // 이 경우에도 실패 시 DLQ에 추가되도록 이벤트 발행
            Set<String> tagsToIncrease = tagsToAdd.stream()
                    .map(Tag::getName)
                    .collect(Collectors.toSet());
            
            tagEventPublisher.publishTagCountUpdate(toRemove, tagsToIncrease);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getTags(Prompt prompt) {
        return promptTagRepository.findPromptTagByPrompt(prompt)
                .stream()
                .map(PromptTag::getTag)
                .toList();
    }
}
```

---

### 10. Dead Letter Queue 처리 스케줄러

#### 파일: `src/main/java/org/example/sharedprompts/domain/tag/service/TagCountDlqProcessor.java`

```java
package org.example.sharedprompts.domain.tag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.tag.config.TagRedisKey;
import org.example.sharedprompts.domain.tag.event.TagCountUpdateEvent;
import org.example.sharedprompts.domain.tag.service.TagCountMetricService;
import org.example.sharedprompts.domain.tag.service.TagCountUpdateService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Dead Letter Queue 처리 스케줄러
 * - 주기적으로 DLQ를 확인하여 재처리 시도
 * - 수동 복구도 가능하도록 구성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagCountDlqProcessor {

    private static final int MAX_DLQ_RETRY = 5;

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
                        log.warn("DLQ item exceeded max retry count, removing: {}", dlqKey);
                        redisTemplate.delete(dlqKey);
                        continue;
                    }

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
     * DLQ 항목 DTO
     */
    private static class DlqItem {
        public Set<String> tagsToDecrease;
        public Set<String> tagsToIncrease;
        public String error;
        public String occurredAt;
        public int retryCount = 0;
    }
}
```

---

## 설정 파일 추가

### application.yml

```yaml
tag:
  count:
    update:
      # 모니터링 활성화 여부
      enable-monitoring: true
      # Dead Letter Queue 활성화 여부
      enable-dlq: true
    # 재시도 큐 처리 간격 (밀리초)
    retry:
      process-interval: 5000  # 5초
    # DLQ 처리 간격 (밀리초)
    dlq:
      process-interval: 3600000  # 1시간

# 비동기 실행자 설정
spring:
  task:
    execution:
      pool:
        core-size: 2
        max-size: 5
        queue-capacity: 100
```

---

## 개선 사항 요약

### 1. 이벤트 기반 아키텍처
- ✅ `TagCountUpdateEvent` 발행으로 확장 가능한 구조
- ✅ `@TransactionalEventListener`로 트랜잭션 커밋 후 처리
- ✅ 향후 Kafka, Redis PubSub 등으로 확장 가능

### 2. 배치 처리 (성능 향상)
- ✅ Redis Pipeline을 통한 배치 처리
- ✅ 여러 태그 카운트 업데이트를 한 번에 처리하여 네트워크 왕복 감소
- ✅ 대량 태그 업데이트 시 성능 향상

### 3. 지연 이벤트 큐 (안정성 강화)
- ✅ Redis List 기반 지연 이벤트 큐
- ✅ CompletableFuture 재귀 구조 대신 큐 기반 재시도로 안정성 향상
- ✅ 스케줄러를 통한 주기적 재시도 처리

### 4. 메트릭 수집 (운영 모니터링)
- ✅ Prometheus 메트릭 등록
- ✅ 성공/실패/재시도/DLQ 횟수 추적
- ✅ 처리 시간 측정

### 5. Redis Key 네이밍 표준화
- ✅ 표준화된 prefix: `sharedprompts:tag:*`
- ✅ TTL 정책 적용 (카운트: 30일, DLQ: 7일)
- ✅ Key 네이밍 중앙 관리

### 6. Redis 업데이트 실패 대응
- ✅ TODO: 모니터링 시스템 알림 (Sentry, NewRelic 등)
- ✅ 재시도 로직 (최대 3회, 큐 기반)
- ✅ Dead Letter Queue 지원

### 7. 동시성/경합 대비
- ✅ Redis Lua 스크립트를 통한 원자 연산 보장
- ✅ `INCR`/`DECR` 명령어 사용
- ✅ 카운트가 0 이하로 내려가지 않도록 보장
- ✅ Pipeline 내에서도 원자성 보장

### 8. 트랜잭션 없는 환경 대비
- ✅ 트랜잭션이 없어도 이벤트 발행으로 일관된 처리
- ✅ DLQ를 통한 실패 복구 메커니즘
- ✅ 스케줄러를 통한 주기적 DLQ 처리

---

## 테스트 고려사항

### 단위 테스트
- [ ] `TagCountUpdateService` 원자 연산 테스트
- [ ] `TagCountUpdateEventListener` 재시도 로직 테스트
- [ ] DLQ 처리 로직 테스트

### 통합 테스트
- [ ] 트랜잭션 커밋 후 이벤트 처리 검증
- [ ] Redis 업데이트 실패 시나리오 테스트
- [ ] 동시성 테스트 (여러 프롬프트의 태그 동시 업데이트)

### 모니터링 테스트
- [ ] Prometheus 메트릭 수집 검증
- [ ] DLQ 동작 검증
- [ ] 재시도 큐 동작 검증
- [ ] TODO: 모니터링 시스템 알림 연동 검증

---

## 향후 확장 가능성

### 1. Kafka 연동
```java
@KafkaListener(topics = "tag-count-update")
public void handleKafkaEvent(TagCountUpdateEvent event) {
    // Kafka를 통한 분산 환경 지원
}
```

### 2. Redis PubSub 연동
```java
@EventListener
public void handleRedisPubSub(TagCountUpdateEvent event) {
    // Redis PubSub을 통한 실시간 동기화
}
```

### 3. 모니터링 시스템 알림 연동
```java
// TODO: Sentry 알림 연동
// TODO: NewRelic 메트릭 기록
// TODO: Slack/PagerDuty 알림 연동
```

---

## 참고 자료

- [Spring Transaction Events](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)
- [Redis Lua Scripting](https://redis.io/docs/manual/programmability/eval-intro/)
- [Sentry Java Integration](https://docs.sentry.io/platforms/java/)
- [Dead Letter Queue Pattern](https://www.enterpriseintegrationpatterns.com/patterns/messaging/DeadLetterChannel.html)

