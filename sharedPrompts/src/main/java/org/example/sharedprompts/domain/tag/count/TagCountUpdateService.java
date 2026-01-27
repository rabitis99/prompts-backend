package org.example.sharedprompts.domain.tag.count;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.tag.config.TagRedisKey;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 태그 카운트 업데이트 전용 서비스
 * - Redis 원자 연산 보장
 * - 태그별 원자 연산을 순차적으로 수행
 * - 실패 시 모니터링 및 재시도 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagCountUpdateService {

    private final StringRedisTemplate redisTemplate;
    private final TagCountMetricService metricService;
    
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
                throw new TagCountUpdateException(
                        "Redis script returned null for tag: " + tagName,
                        new IllegalStateException("Null result from Redis script")
                );
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
                throw new TagCountUpdateException(
                        "Redis script returned null for tag: " + tagName,
                        new IllegalStateException("Null result from Redis script")
                );
            }
            
            log.debug("Tag count decremented: {} -> {}", tagName, result);
            return result;
        } catch (Exception e) {
            log.error("Failed to decrement tag count for {}: {}", tagName, e.getMessage(), e);
            throw new TagCountUpdateException("Failed to decrement tag count: " + tagName, e);
        }
    }

    /**
     * 태그 카운트 업데이트 (배치 처리)
     * 
     * @param tagsToDecrease 감소할 태그 목록
     * @param tagsToIncrease 증가할 태그 목록
     * @throws TagCountUpdateException 부분 실패 시 예외 발생 (재시도/DLQ 활성화)
     */
    public void updateTagCounts(Set<String> tagsToDecrease, Set<String> tagsToIncrease) {
        if (tagsToDecrease.isEmpty() && tagsToIncrease.isEmpty()) {
            return;
        }

        List<String> failedDecreases = new ArrayList<>();
        List<String> failedIncreases = new ArrayList<>();
        Exception firstException = null;

        // 감소 처리
        for (String tagName : tagsToDecrease) {
            try {
                decrementTagCount(tagName);
                // 태그별 메트릭 기록 (tag_name 레이블 포함)
                metricService.recordTagSuccess("decrease");
            } catch (Exception e) {
                log.error("Failed to decrement tag count for {}: {}", tagName, e.getMessage(), e);
                failedDecreases.add(tagName);
                if (firstException == null) {
                    firstException = e;
                }
            }
        }
        
        // 증가 처리
        for (String tagName : tagsToIncrease) {
            try {
                incrementTagCount(tagName);
                // 태그별 메트릭 기록 (tag_name 레이블 포함)
                metricService.recordTagSuccess("increase");
            } catch (Exception e) {
                log.error("Failed to increment tag count for {}: {}", tagName, e.getMessage(), e);
                failedIncreases.add(tagName);
                if (firstException == null) {
                    firstException = e;
                }
            }
        }

        // 부분 실패가 있으면 예외를 발생시켜 재시도/DLQ 메커니즘 활성화
        if (!failedDecreases.isEmpty() || !failedIncreases.isEmpty()) {
            throw new TagCountUpdateException(
                    String.format("Tag count update failed. decrease=%s, increase=%s", 
                            failedDecreases, failedIncreases),
                    firstException
            );
        }

        log.debug("Tag count batch update completed: decrease={}, increase={}",
                tagsToDecrease.size(), tagsToIncrease.size());
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

