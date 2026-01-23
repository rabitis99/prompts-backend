package org.example.sharedprompts.auth.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * Token Version 저장소 구현체
 * 
 * Redis를 사용하여 사용자별 tokenVersion을 관리합니다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TokenVersionStoreImpl implements TokenVersionStore {

    private static final String DEFAULT_VERSION = "0";
    private static final int DEFAULT_TTL_DAYS = 365;

    private final StringRedisTemplate redisTemplate;

    @Override
    public Long get(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 null일 수 없습니다.");
        }

        try {
            String key = RedisKeyFactory.tokenVersion(userId);
            String value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                return 0L;
            }
            
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.error("Token Version 파싱 실패: userId={}", userId, e);
            return 0L;
        }
    }

    @Override
    public void increment(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 null일 수 없습니다.");
        }

        try {
            String key = RedisKeyFactory.tokenVersion(userId);
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, Duration.ofDays(DEFAULT_TTL_DAYS));
        } catch (Exception e) {
            log.error("Token Version 증가 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void initialize(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 null일 수 없습니다.");
        }

        try {
            String key = RedisKeyFactory.tokenVersion(userId);
            Boolean set = redisTemplate.opsForValue().setIfAbsent(
                    key,
                    DEFAULT_VERSION,
                    Duration.ofDays(DEFAULT_TTL_DAYS)
            );
            
            // setIfAbsent가 null을 반환할 때 실패로 처리
            if (set == null) {
                log.error("토큰 버전 초기화 결과가 null: userId={}", userId);
                throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            
            if (Boolean.FALSE.equals(set)) {
                log.debug("토큰 버전 초기화 스킵(이미 존재): userId={}", userId);
            } else {
                log.debug("토큰 버전 초기화 완료: userId={}", userId);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token Version 초기화 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void delete(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 null일 수 없습니다.");
        }

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
