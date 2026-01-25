package org.example.sharedprompts.auth.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.storage.RedisKeyFactory;
import org.example.sharedprompts.global.redis.RedisHealthService;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.dao.DataAccessException;

import java.util.function.Supplier;

/**
 * Redis 관련 공통 유틸리티
 * 
 * Redis Key 생성, 안전한 Redis 호출 래퍼, 예외 처리 로직을 공통화합니다.
 * Fail-Open 정책을 일괄 적용하여 Redis 장애 시 서비스 연속성을 보장합니다.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RedisUtil {

    /**
     * Access Token Redis Key 생성
     * 
     * @param token Access Token
     * @return Redis Key
     */
    public static String accessTokenKey(String token) {
        return RedisKeyFactory.accessToken(token);
    }

    /**
     * Refresh Token Redis Key 생성
     * 
     * @param token Refresh Token
     * @return Redis Key
     */
    public static String refreshTokenKey(String token) {
        return RedisKeyFactory.refreshToken(token);
    }

    /**
     * Redis 호출을 안전하게 처리하는 래퍼 메서드
     * 
     * <p>Fail-Open 정책 적용:
     * <ul>
     *   <li>읽기 작업: Redis 장애 시 fallback 값 반환 (서비스 연속성 보장)</li>
     *   <li>쓰기 작업: Redis 장애 시 예외 발생 (데이터 일관성 보장)</li>
     * </ul>
     * 
     * <p>사용 예시:
     * <pre>{@code
     * // 읽기 작업 (Fail-Open)
     * boolean isValid = RedisUtil.safeCall(
     *     () -> redisTemplate.hasKey(RedisUtil.accessTokenKey(token)),
     *     true  // Redis 장애 시 true 반환 (Fail-Open)
     * );
     * 
     * // 쓰기 작업 (Fail-Close)
     * RedisUtil.safeCall(
     *     () -> {
     *         redisTemplate.opsForValue().set(key, value, ttl);
     *         return null;
     *     },
     *     null
     * );
     * }</pre>
     * 
     * @param <T> 반환 타입
     * @param redisCall Redis 호출 로직
     * @param fallback Redis 장애 시 반환할 fallback 값
     * @return Redis 호출 결과 또는 fallback 값
     */
    public static <T> T safeCall(Supplier<T> redisCall, T fallback) {
        try {
            return redisCall.get();
        } catch (DataAccessException e) {
            log.warn("Redis 호출 실패, Fail-Open 정책 적용: {}", e.getMessage(), e);
            return fallback;
        } catch (Exception e) {
            log.error("Redis 호출 중 예상치 못한 예외 발생: {}", e.getMessage(), e);
            return fallback;
        }
    }

    /**
     * Redis 호출을 안전하게 처리하는 래퍼 메서드 (예외 발생)
     * 
     * <p>쓰기 작업 등 데이터 일관성이 중요한 경우 사용합니다.
     * Redis 장애 시 예외를 발생시켜 트랜잭션 롤백 등을 보장합니다.
     * 
     * <p>트랜잭션 롤백 적용 여부:
     * <ul>
     *   <li>@Transactional 메서드 내에서 호출 시: RuntimeException이므로 트랜잭션 롤백 발생</li>
     *   <li>트랜잭션 없이 호출 시: 예외만 발생하고 롤백 없음</li>
     *   <li>주의: Redis 장애로 인한 예외는 비즈니스 로직 예외이므로 트랜잭션 롤백이 적절함</li>
     * </ul>
     * 
     * @param <T> 반환 타입
     * @param redisCall Redis 호출 로직
     * @param errorMessage 예외 발생 시 사용할 에러 메시지
     * @return Redis 호출 결과
     * @throws RuntimeException Redis 장애 시 발생 (트랜잭션 롤백 유발)
     */
    public static <T> T safeCallOrThrow(Supplier<T> redisCall, String errorMessage) {
        try {
            return redisCall.get();
        } catch (DataAccessException e) {
            log.error("Redis 호출 실패 (데이터 일관성 보장을 위해 예외 발생): {}", errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        } catch (Exception e) {
            log.error("Redis 호출 중 예상치 못한 예외 발생: {}", errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * Redis 호출 실패를 로깅하고 Health Service에 보고
     * 
     * <p>리플렉션 대신 직접 메서드 호출로 변경하여 성능 및 타입 안정성 향상
     * 
     * @param redisCall Redis 호출 로직
     * @param fallback Redis 장애 시 반환할 fallback 값
     * @param healthService Redis Health Service (null 가능)
     * @param token 마스킹할 토큰 (로깅용, null 가능)
     * @param <T> 반환 타입
     * @return Redis 호출 결과 또는 fallback 값
     */
    public static <T> T safeCallWithHealthCheck(
            Supplier<T> redisCall,
            T fallback,
            RedisHealthService healthService,
            String token) {
        try {
            return redisCall.get();
        } catch (DataAccessException e) {
            String maskedToken = token != null ? SensitiveDataMasker.maskToken(token) : "N/A";
            log.warn("Redis 호출 실패, Fail-Open 정책 적용: token={}", maskedToken, e);
            
            // Health Service에 실패 보고 (직접 메서드 호출)
            if (healthService != null) {
                healthService.reportFailure();
            }
            
            return fallback;
        } catch (Exception e) {
            log.error("Redis 호출 중 예상치 못한 예외 발생", e);
            
            // Health Service에 실패 보고 (직접 메서드 호출)
            if (healthService != null) {
                healthService.reportFailure();
            }
            
            return fallback;
        }
    }
}

