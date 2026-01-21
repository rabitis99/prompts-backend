package org.example.sharedprompts.domain.shared.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.Lua.LuaScripts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class BaseCountServiceImpl implements BaseCountService {

    private static final Logger log = LoggerFactory.getLogger(BaseCountServiceImpl.class);
    // Hot data TTL (e.g. 1 day) - 활성 데이터만 Redis에 유지하기 위한 만료 시간
    private static final long HOT_TTL_SECONDS = 24 * 60 * 60L;

    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<Long> safeDecrScript;

    @PostConstruct
    public void init() {
        safeDecrScript = new DefaultRedisScript<>();
        safeDecrScript.setScriptText(LuaScripts.SAFE_DECREMENT);
        safeDecrScript.setResultType(Long.class);
    }

    @Override
    public void increment(String key) {
        try {
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, java.time.Duration.ofSeconds(HOT_TTL_SECONDS));
        } catch (Exception e) {
            log.error("Redis increment operation failed for key: {}", key, e);
            throw e;
        }
    }

    @Override
    public void decrement(String key) {
        try {
            redisTemplate.execute(safeDecrScript, List.of(key));
            redisTemplate.expire(key, java.time.Duration.ofSeconds(HOT_TTL_SECONDS));
        } catch (Exception e) {
            log.error("Redis decrement operation failed for key: {}", key, e);
            throw e;
        }
    }

    @Override
    public long incrementAndGet(String key) {
        try {
            Long val = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, java.time.Duration.ofSeconds(HOT_TTL_SECONDS));
            return val != null ? val : 0L;
        } catch (Exception e) {
            log.error("Redis incrementAndGet operation failed for key: {}", key, e);
            throw e;
        }
    }

    @Override
    public long decrementAndGet(String key) {
        try {
            Long val = redisTemplate.execute(safeDecrScript, List.of(key));
            redisTemplate.expire(key, java.time.Duration.ofSeconds(HOT_TTL_SECONDS));
            return val != null ? val : 0L;
        } catch (Exception e) {
            log.error("Redis decrementAndGet operation failed for key: {}", key, e);
            throw e;
        }
    }

    @Override
    public void set(String key, long value) {
        try {
            redisTemplate.opsForValue().set(key, Long.toString(value));
            redisTemplate.expire(key, java.time.Duration.ofSeconds(HOT_TTL_SECONDS));
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

