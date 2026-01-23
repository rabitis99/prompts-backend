package org.example.sharedprompts.auth.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * TokenVersionStore의 Redis 구현
 * 
 * 저장 형식: auth:version:{userId} = {version}
 * TTL: 365일 (사용자 삭제 시 수동 삭제, TTL은 안전장치 역할)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenVersionStoreImpl implements TokenVersionStore {

    private static final long DEFAULT_TTL_DAYS = 365; // 1년 (실제로는 수동 삭제)

    private final StringRedisTemplate redisTemplate;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    public Long get(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        
        String key = RedisKeyFactory.tokenVersion(userId);
        String value;
        
        try {
            value = redisTemplate.opsForValue().get(key);
        } catch (DataAccessException e) {
            log.error("토큰 버전 조회 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        
        if (value == null) {
            // 캐시에 없으면 0 반환 (초기 버전)
            return 0L;
        }
        
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            log.error("토큰 버전 형식 오류: userId={}, value={}", userId, value, e);
            throw new ApiException(ErrorCode.TOKEN_VERSION_INCREMENT_FAILED);
        }
    }

    @Override
    public void increment(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        
        String key = RedisKeyFactory.tokenVersion(userId);
        Long newVersion;
        
        try {
            newVersion = redisTemplate.opsForValue().increment(key);
        } catch (DataAccessException e) {
            log.error("토큰 버전 증가 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        
        if (newVersion == null) {
            log.error("토큰 버전 증가 실패: userId={}", userId);
            throw new ApiException(ErrorCode.TOKEN_VERSION_INCREMENT_FAILED);
        }
        
        // 새로 생성된 경우(버전이 1) TTL 설정
        // 기존 키의 경우 TTL이 이미 설정되어 있으므로 재설정 불필요
        if (newVersion == 1) {
            try {
                redisTemplate.expire(key, Duration.ofDays(DEFAULT_TTL_DAYS));
            } catch (DataAccessException e) {
                log.error("토큰 버전 TTL 설정 실패: userId={}", userId, e);
                // TTL 설정 실패는 치명적이지 않으므로 경고만
            }
        }
        
        log.debug("토큰 버전 증가: userId={}, newVersion={}", userId, newVersion);
    }

    @Override
    public void initialize(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        
        String key = RedisKeyFactory.tokenVersion(userId);
        
        try {
            Boolean set = redisTemplate.opsForValue().setIfAbsent(
                    key, "0", Duration.ofDays(DEFAULT_TTL_DAYS)
            );
            if (Boolean.FALSE.equals(set)) {
                log.debug("토큰 버전 초기화 스킵(이미 존재): userId={}", userId);
            } else {
                log.debug("토큰 버전 초기화: userId={}", userId);
            }
        } catch (DataAccessException e) {
            log.error("토큰 버전 초기화 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void delete(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        
        String key = RedisKeyFactory.tokenVersion(userId);
        
        try {
            redisTemplate.delete(key);
            
            // 관련 토큰도 삭제
            refreshTokenStore.deleteAllByUser(userId);
            
            log.info("토큰 버전 삭제: userId={}", userId);
        } catch (DataAccessException e) {
            log.error("토큰 버전 삭제 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}

