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
        } catch (Exception e) {
            log.error("Redis increment operation failed for key: {}", key, e);
            throw e;
        }
    }

    @Override
    public void decrement(String key) {
        try {
            redisTemplate.execute(safeDecrScript, List.of(key));
        } catch (Exception e) {
            log.error("Redis decrement operation failed for key: {}", key, e);
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

