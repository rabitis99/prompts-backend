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
     * Lua 스크립트 객체를 정적 필드로 캐시하여 재사용합니다.
     * 매 호출마다 생성하는 비용을 줄여 성능을 개선합니다.
     */
    private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL_SCRIPT;

    static {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LuaScripts.INCREMENT_WITH_TTL);
        script.setResultType(Long.class);
        INCREMENT_WITH_TTL_SCRIPT = script;
    }
    
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

            // Redis 스크립트가 null을 반환하는 것은 비정상 상황입니다.
            // INCREMENT_WITH_TTL Lua 스크립트는 정상적으로 항상 값을 반환하므로,
            // null은 Redis 장애, 스크립트 실행 오류, 또는 연결 문제를 의미합니다.
            if (currentCount == null) {
                throw new IllegalStateException("Redis script returned null for key: " + key);
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
        return redisTemplate.execute(
                INCREMENT_WITH_TTL_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(windowSeconds)
        );
    }
}

