package org.example.sharedprompts.auth.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.Lua.LuaScripts;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;

/**
 * Token Version 저장소 구현체
 *
 * Redis를 사용하여 사용자별 tokenVersion을 관리하며,
 * Lua 스크립트를 활용해 INCR + EXPIRE를 원자적으로 처리합니다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TokenVersionStoreImpl implements TokenVersionStore {

    private static final String DEFAULT_VERSION = "0";
    private static final int DEFAULT_TTL_DAYS = 365;

    private final StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long> incrementWithTtlScript = createIncrementScript();

    private static DefaultRedisScript<Long> createIncrementScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LuaScripts.INCREMENT_WITH_TTL);
        script.setResultType(Long.class);
        return script;
    }

    private void assertUserId(Long userId) {
        if (userId == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "userId", "userId는 null일 수 없습니다.");
        }
    }

    @Override
    public Long get(Long userId) {
        assertUserId(userId);

        try {
            String key = RedisKeyFactory.tokenVersion(userId);
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) return 0L;

            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Token Version 파싱 실패(userId={}): 초기값 0으로 반환", userId, e);
            return 0L;
        }
    }

    @Override
    public Long increment(Long userId) {
        assertUserId(userId);

        try {
            String key = RedisKeyFactory.tokenVersion(userId);
            long ttlSeconds = Duration.ofDays(DEFAULT_TTL_DAYS).getSeconds();

            Long newVersion = redisTemplate.execute(
                    incrementWithTtlScript,
                    Collections.singletonList(key),
                    String.valueOf(ttlSeconds)
            );

            log.debug("Token Version 증가 완료: userId={}, newVersion={}", userId, newVersion);
            return newVersion;
        } catch (Exception e) {
            log.error("Token Version 증가 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void initialize(Long userId) {
        assertUserId(userId);

        try {
            String key = RedisKeyFactory.tokenVersion(userId);
            Boolean set = redisTemplate.opsForValue().setIfAbsent(
                    key,
                    DEFAULT_VERSION,
                    Duration.ofDays(DEFAULT_TTL_DAYS)
            );

            if (Boolean.TRUE.equals(set)) {
                log.debug("Token Version 초기화 완료: userId={}", userId);
            } else {
                log.debug("Token Version 초기화 스킵(이미 존재): userId={}", userId);
            }
        } catch (Exception e) {
            log.error("Token Version 초기화 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void delete(Long userId) {
        assertUserId(userId);

        try {
            String key = RedisKeyFactory.tokenVersion(userId);
            redisTemplate.delete(key);
            log.debug("Token Version 삭제 완료: userId={}", userId);
        } catch (Exception e) {
            log.error("Token Version 삭제 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
