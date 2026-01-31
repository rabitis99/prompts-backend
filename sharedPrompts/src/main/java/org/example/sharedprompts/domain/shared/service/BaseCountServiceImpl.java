package org.example.sharedprompts.domain.shared.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class BaseCountServiceImpl implements BaseCountService {

    private static final Logger log = LoggerFactory.getLogger(BaseCountServiceImpl.class);

    private final StringRedisTemplate redisTemplate;

    // Hot data TTL (seconds) - 활성 데이터만 Redis에 유지하기 위한 만료 시간
    @Value("${app.redis.hot-ttl-seconds:86400}")
    private long hotTtlSeconds;

    /**
     * INCREMENT_WITH_TTL Lua 스크립트
     * 
     * 책임 분리:
     * - 스크립트 정의: RedisLuaScriptConfig
     * - 스크립트 실행: 이 클래스 (Service 책임)
     * 
     * 운영 원칙:
     * - @PostConstruct에서 초기화하지 않음 (외부 리소스 접근 위험 제거)
     * - Bean 주입으로 스크립트 사용 (애플리케이션 기동 안정성 보장)
     * 
     * 반환값: List<Long> [currentCount, ttl] 배열
     * - currentCount: INCR 후의 현재 카운트 값
     * - ttl: 키의 남은 TTL (초 단위), 키가 없거나 TTL이 없으면 -1
     */
    private final DefaultRedisScript<List<Long>> incrementWithTtlScript;
    
    /**
     * SAFE_DECREMENT_WITH_TTL Lua 스크립트
     */
    private final DefaultRedisScript<Long> safeDecrementWithTtlScript;

    @Override
    public void increment(String key) {
        executeWithTtl(incrementWithTtlScript, key, "increment");
    }

    @Override
    public void decrement(String key) {
        executeWithTtlLong(safeDecrementWithTtlScript, key, "decrement");
    }

    @Override
    public long incrementAndGet(String key) {
        List<Long> result = executeWithTtl(incrementWithTtlScript, key, "incrementAndGet");
        // 스크립트는 [currentCount, ttl] 배열을 반환하므로 첫 번째 값만 사용
        return (result != null && !result.isEmpty()) ? result.get(0) : 0L;
    }

    @Override
    public long decrementAndGet(String key) {
        Long val = executeWithTtlLong(safeDecrementWithTtlScript, key, "decrementAndGet");
        return val != null ? val : 0L;
    }

    @Override
    public void set(String key, long value) {
        try {
            redisTemplate
                    .opsForValue()
                    .set(
                            key,
                            Long.toString(value),
                            Duration.ofSeconds(hotTtlSeconds)
                    );
        } catch (Exception e) {
            log.error("Redis set operation failed for key: {}", key, e);
            throw e;
        }
    }

    @Override
    public Map<Long, Long> getCounts(List<Long> ids, Function<Long, String> keyMapper) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        
        List<String> keys = ids.stream().map(keyMapper).toList();
        List<String> valuesRaw = redisTemplate.opsForValue().multiGet(keys);

        // null 체크 및 길이 불일치 대응
        final List<String> values = (valuesRaw == null || valuesRaw.size() != ids.size())
                ? keys.stream().map(k -> "0").toList()
                : valuesRaw;

        Map<Long, Long> result = new HashMap<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            result.put(ids.get(i), safeParse(values.get(i)));
        }
        return result;
    }

    private List<Long> executeWithTtl(
            DefaultRedisScript<List<Long>> script,
            String key,
            String operationName
    ) {
        try {
            return redisTemplate.execute(
                    script,
                    List.of(key),
                    String.valueOf(hotTtlSeconds)
            );
        } catch (Exception e) {
            log.error("Redis {} operation failed for key: {}", operationName, key, e);
            throw e;
        }
    }

    private Long executeWithTtlLong(
            DefaultRedisScript<Long> script,
            String key,
            String operationName
    ) {
        try {
            return redisTemplate.execute(
                    script,
                    List.of(key),
                    String.valueOf(hotTtlSeconds)
            );
        } catch (Exception e) {
            log.error("Redis {} operation failed for key: {}", operationName, key, e);
            throw e;
        }
    }

    protected Long safeParse(String v) {
        if (v == null) {
            return 0L;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}

