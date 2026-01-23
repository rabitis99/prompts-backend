package org.example.sharedprompts.auth.rate;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.Lua.LuaScripts;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 고정 윈도우(Fixed Window) 방식 Rate Limiter 구현체.
 *
 * - LuaScripts.INCREMENT_WITH_TTL 스크립트를 사용해 INCR + EXPIRE를 원자적으로 수행합니다.
 * - 고정 윈도우(window) 방식으로 분 단위 요청 횟수를 제한합니다.
 * - 시간 윈도우가 고정되어 있어 윈도우 경계에서 버스트가 발생할 수 있습니다.
 */
@Component
@RequiredArgsConstructor
public class FixedWindowRateLimiter implements RateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 주어진 key에 대해 1토큰을 소비하면서 요청 허용 여부를 판단합니다.
     *
     * @param key          rate limit key (예: rate:login:ip:1.2.3.4)
     * @param capacity     윈도우 내 허용 횟수
     * @param windowSeconds 윈도우(초)
     * @return RateLimitResult (허용 여부, 현재 카운트, Retry-After 초)
     */
    @Override
    public RateLimitResult consume(String key, long capacity, long windowSeconds) {
        try {
            Long currentCount = executeIncrementWithTtl(key, windowSeconds);

            if (currentCount == null) {
                currentCount = 0L;
            }

            boolean allowed = currentCount <= capacity;

            long retryAfter = 0L;
            if (!allowed) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                if (ttl == null || ttl < 0) {
                    ttl = windowSeconds;
                }
                retryAfter = ttl;
            }

            return new RateLimitResult(allowed, currentCount, retryAfter);
        } catch (Exception e) {
            // Redis 장애 시 예외를 상위로 전파하여 Fail Open 정책 적용
            throw new RateLimitException("Rate limit check failed for key: " + key, e);
        }
    }

    private Long executeIncrementWithTtl(String key, long windowSeconds) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LuaScripts.INCREMENT_WITH_TTL);
        script.setResultType(Long.class);

        return redisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(windowSeconds)
        );
    }
}

