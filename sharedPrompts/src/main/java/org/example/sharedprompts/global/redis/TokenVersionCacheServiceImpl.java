package org.example.sharedprompts.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * TokenVersionCacheService의 Redis 구현
 * 
 * 저장 형식: TOKEN_VERSION:{userId} = {version}
 * TTL: 365일 (사용자 삭제 시 수동 삭제, TTL은 안전장치 역할)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenVersionCacheServiceImpl implements TokenVersionCacheService {

    private static final String PREFIX = "TOKEN_VERSION:";
    private static final long DEFAULT_TTL_DAYS = 365; // 1년 (실제로는 수동 삭제)

    private final StringRedisTemplate redisTemplate;

    @Override
    public Long getTokenVersion(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        String key = PREFIX + userId;
        String value = redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            // 캐시에 없으면 0 반환 (초기 버전)
            return 0L;
        }
        
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn("토큰 버전 형식 오류: userId={}, value={}", userId, value);
            return 0L;
        }
    }

    @Override
    public void incrementTokenVersion(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        String key = PREFIX + userId;
        Long newVersion = redisTemplate.opsForValue().increment(key);
        
        if (newVersion == null) {
            log.error("토큰 버전 증가 실패: userId={}", userId);
            throw new ApiException(ErrorCode.TOKEN_VERSION_INCREMENT_FAILED);
        }
        
        // 새로 생성된 경우(버전이 1) TTL 설정
        // 기존 키의 경우 TTL이 이미 설정되어 있으므로 재설정 불필요
        if (newVersion == 1) {
            redisTemplate.expire(key, Duration.ofDays(DEFAULT_TTL_DAYS));
        }
        
        log.debug("토큰 버전 증가: userId={}, newVersion={}", userId, newVersion);
    }

    @Override
    public void initializeTokenVersion(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        String key = PREFIX + userId;
        redisTemplate.opsForValue().set(key, "0", Duration.ofDays(DEFAULT_TTL_DAYS));
        log.debug("토큰 버전 초기화: userId={}", userId);
    }

    @Override
    public void deleteTokenVersion(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        String key = PREFIX + userId;
        redisTemplate.delete(key);
        log.info("토큰 버전 삭제: userId={}", userId);
    }
}

