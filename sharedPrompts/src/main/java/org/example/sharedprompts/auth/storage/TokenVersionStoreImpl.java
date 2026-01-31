package org.example.sharedprompts.auth.storage;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.resilience.RedisExecutor;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.redis.RedisFailSafeHandler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

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
    private final RedisFailSafeHandler redisFailSafeHandler;
    
    /**
     * INCREMENT_WITH_TTL Lua 스크립트
     * 
     * 책임 분리:
     * - 스크립트 정의: RedisLuaScriptConfig
     * - 스크립트 실행: 이 클래스 (Repository 책임)
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

    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "getFallback")
    public Long get(Long userId) {
        if (userId == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "userId", "userId는 null일 수 없습니다.");
        }

        Long result = redisFailSafeHandler.executeRead(
                () -> {
                    String key = RedisKeyFactory.tokenVersion(userId);
                    String value = redisTemplate.opsForValue().get(key);
                    
                    if (value == null) {
                        return 0L;
                    }
                    
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        log.error("Token Version 파싱 실패: userId={}", userId, e);
                        return 0L;
                    }
                },
                RedisExecutor.ReadType.NULL_LONG, // Fail-Close: null 반환 후 0으로 변환
                "TokenVersion 조회: userId=" + userId
        );

        return result != null ? result : 0L;
    }
    
    /**
     * TokenVersion 조회 Fallback (Circuit Breaker Open 상태)
     * 
     * <p>Redis 장애 시 0 반환 (Fail-Open: 기본값으로 처리)
     */
    private Long getFallback(Long userId, Exception e) {
        log.warn("Circuit Breaker Open: TokenVersion 조회 실패 (Redis 장애) - userId={}, 기본값 0 반환", userId, e);
        return 0L; // Fail-Open: 기본값
    }

    /**
     * 사용자의 tokenVersion 증가
     * 
     * <p>쓰기 작업이므로 Redis 장애 시 예외 발생 (데이터 일관성 보장)
     * 
     * @param userId 사용자 ID
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "incrementFallback")
    public void increment(Long userId) {
        if (userId == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "userId", "userId는 null일 수 없습니다.");
        }

        redisFailSafeHandler.executeWrite(
                () -> {
                    String key = RedisKeyFactory.tokenVersion(userId);
                    // Lua 스크립트를 사용하여 INCR + EXPIRE를 원자적으로 수행
                    // TTL은 초 단위로 전달 (365일 = 365 * 24 * 60 * 60 초)
                    long ttlSeconds = Duration.ofDays(DEFAULT_TTL_DAYS).getSeconds();
                    // 스크립트는 [currentCount, ttl] 배열을 반환하지만, 여기서는 반환값을 사용하지 않음
                    redisTemplate.execute(
                            incrementWithTtlScript,
                            Collections.singletonList(key),
                            String.valueOf(ttlSeconds)
                    );
                },
                "TokenVersion 증가: userId=" + userId
        );
    }
    
    /**
     * TokenVersion 증가 Fallback (Circuit Breaker Open 상태)
     */
    private void incrementFallback(Long userId, Exception e) {
        log.error("Circuit Breaker Open: TokenVersion 증가 실패 (Redis 장애) - userId={}", userId, e);
        throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Redis 장애로 인해 TokenVersion 증가에 실패했습니다.");
    }

    /**
     * 사용자의 tokenVersion 초기화 (회원가입 시)
     * 
     * <p>쓰기 작업이므로 Redis 장애 시 예외 발생 (데이터 일관성 보장)
     * 
     * @param userId 사용자 ID
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "initializeFallback")
    public void initialize(Long userId) {
        if (userId == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "userId", "userId는 null일 수 없습니다.");
        }

        redisFailSafeHandler.executeWrite(
                () -> {
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
                },
                "TokenVersion 초기화: userId=" + userId
        );
    }
    
    /**
     * TokenVersion 초기화 Fallback (Circuit Breaker Open 상태)
     */
    private void initializeFallback(Long userId, Exception e) {
        log.error("Circuit Breaker Open: TokenVersion 초기화 실패 (Redis 장애) - userId={}", userId, e);
        throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Redis 장애로 인해 TokenVersion 초기화에 실패했습니다.");
    }

    /**
     * 사용자의 tokenVersion 삭제
     * 
     * <p>삭제 작업이므로 Redis 장애 시에도 예외를 던지지 않음
     * 
     * @param userId 사용자 ID
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "deleteFallback")
    public void delete(Long userId) {
        if (userId == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "userId", "userId는 null일 수 없습니다.");
        }

        redisFailSafeHandler.executeDelete(
                () -> {
                    String key = RedisKeyFactory.tokenVersion(userId);
                    redisTemplate.delete(key);
                    log.debug("Token Version 삭제 완료: userId={}", userId);
                },
                "TokenVersion 삭제: userId=" + userId
        );
    }
    
    /**
     * TokenVersion 삭제 Fallback (Circuit Breaker Open 상태)
     */
    private void deleteFallback(Long userId, Exception e) {
        // 삭제 실패는 치명적이지 않으므로 예외를 던지지 않음
        log.debug("Circuit Breaker Open: TokenVersion 삭제 실패 (Redis 장애) - userId={}, 무시하고 계속 진행", userId);
    }
}


