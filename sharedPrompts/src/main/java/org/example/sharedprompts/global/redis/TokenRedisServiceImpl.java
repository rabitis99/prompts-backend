package org.example.sharedprompts.global.redis;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.jwt.config.TokenTtlProperties;
import org.example.sharedprompts.auth.resilience.RedisExecutor;
import org.example.sharedprompts.auth.storage.RedisKeyFactory;
import org.example.sharedprompts.global.constant.Constant;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Token Redis 서비스 구현체
 * 
 * Access Token, Refresh Token, OAuth2 임시 토큰을 Redis에 저장하고 관리합니다.
 * 
 * Redis 장애 대응:
 * - Circuit Breaker 패턴 적용으로 장애 격리
 * - 읽기 작업(토큰 검증): Redis 장애 시 Fail-Open 정책 적용 (토큰을 유효한 것으로 간주)
 * - 쓰기 작업(토큰 저장/삭제): Redis 장애 시 예외 발생 (데이터 일관성 보장)
 * - Redis Health Check 연동으로 자동 Fail-Open 정책 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRedisServiceImpl implements TokenRedisService {

    private final StringRedisTemplate redisTemplate;
    private final TokenTtlProperties ttlProperties;
    private final RedisExecutor redisExecutor;

    // ==================== Access Token 관리 ====================

    /**
     * Access Token 저장
     * 
     * 쓰기 작업이므로 Redis 장애 시 예외 발생 (데이터 일관성 보장)
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "saveAccessTokenFallback")
    public void saveAccessToken(String token, Long userId) {
        redisExecutor.executeWrite(
                () -> {
                    String key = RedisKeyFactory.accessToken(token);
                    Duration ttl = Duration.ofMillis(ttlProperties.getAccessTokenValidityMillis());
                    redisTemplate.opsForValue().set(key, String.valueOf(userId), ttl);
                    log.debug("Access Token 저장 완료: userId={}", userId);
                },
                "AccessToken 저장: userId=" + userId
        );
    }
    
    /**
     * Access Token 저장 Fallback (Circuit Breaker Open 상태)
     */
    private void saveAccessTokenFallback(String token, Long userId, Exception e) {
        log.error("Circuit Breaker Open: Access Token 저장 실패 (Redis 장애) - userId={}, token={}", 
                userId, SensitiveDataMasker.maskToken(token), e);
        throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Redis 장애로 인해 토큰 저장에 실패했습니다.");
    }

    /**
     * Access Token 유효성 검증
     * 
     * 읽기 작업이므로 Redis 장애 시 Fail-Open 정책 적용 (토큰을 유효한 것으로 간주)
     * JWT 서명 검증은 이미 통과한 상태이므로, Redis 장애 시에도 인증을 허용하여 서비스 연속성 보장
     * 
     * <p>자동 Fail-Open 정책:
     * <ul>
     *   <li>Redis Health Check가 장애를 감지한 경우, Redis 호출 전에 미리 Fail-Open 적용</li>
     *   <li>Redis 호출 중 DataAccessException 발생 시에도 Fail-Open 적용</li>
     *   <li>Circuit Breaker가 Open 상태일 때도 Fallback에서 Fail-Open 적용</li>
     * </ul>
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "isAccessTokenValidFallback")
    public boolean isAccessTokenValid(String token) {
        return redisExecutor.executeRead(
                () -> {
                    String key = RedisKeyFactory.accessToken(token);
                    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
                },
                RedisExecutor.ReadType.ACCESS_TOKEN,
                "AccessToken 검증: " + SensitiveDataMasker.maskToken(token)
        );
    }
    
    /**
     * Access Token 유효성 검증 Fallback (Circuit Breaker Open 상태)
     * 
     * Redis 장애 시 토큰을 유효한 것으로 간주하여 서비스 연속성 보장
     */
    private boolean isAccessTokenValidFallback(String token, Exception e) {
        return true; // Fail-Open
    }

    /**
     * Access Token 삭제
     * 
     * 쓰기 작업이지만, 삭제 실패는 치명적이지 않으므로 장애 시에도 예외를 던지지 않음
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "deleteAccessTokenFallback")
    public void deleteAccessToken(String token) {
        redisExecutor.executeDelete(
                () -> {
                    String key = RedisKeyFactory.accessToken(token);
                    Boolean deleted = redisTemplate.delete(key);
                    
                    if (Boolean.FALSE.equals(deleted)) {
                        log.debug("삭제할 Access Token이 없음: token={}", SensitiveDataMasker.maskToken(token));
                    }
                },
                "AccessToken 삭제: " + SensitiveDataMasker.maskToken(token)
        );
    }
    
    /**
     * Access Token 삭제 Fallback (Circuit Breaker Open 상태)
     */
    private void deleteAccessTokenFallback(String token, Exception e) {
        // 삭제 실패는 치명적이지 않으므로 예외를 던지지 않음
    }

    /**
     * Access Token 유효성 검증 (UserId 포함)
     * 
     * 읽기 작업이므로 Redis 장애 시 Fail-Open 정책 적용
     * 
     * <p>자동 Fail-Open 정책: Health Check 장애 감지 시 Redis 호출 전에 미리 Fail-Open 적용
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "isAccessTokenValidWithUserIdFallback")
    public boolean isAccessTokenValidWithUserId(String accessToken, Long userId) {
        return redisExecutor.executeRead(
                () -> {
                    String key = RedisKeyFactory.accessToken(accessToken);
                    String storedUserId = redisTemplate.opsForValue().get(key);
                    return storedUserId != null && storedUserId.equals(userId.toString());
                },
                RedisExecutor.ReadType.ACCESS_TOKEN,
                "AccessToken 검증 (userId): userId=" + userId
        );
    }
    
    /**
     * Access Token 유효성 검증 Fallback (UserId 포함, Circuit Breaker Open 상태)
     */
    private boolean isAccessTokenValidWithUserIdFallback(String accessToken, Long userId, Exception e) {
        return true; // Fail-Open
    }

    // ==================== Refresh Token 관리 ====================

    /**
     * Refresh Token 유효성 검증
     * 
     * 읽기 작업이지만 Refresh Token은 보안상 중요하므로 장애 시 false 반환 (Fail-Close)
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "isRefreshTokenValidFallback")
    public boolean isRefreshTokenValid(String token, Long userId) {
        return redisExecutor.executeRead(
                () -> {
                    String key = RedisKeyFactory.refreshToken(token);
                    String storedUserId = redisTemplate.opsForValue().get(key);
                    return storedUserId != null && storedUserId.equals(userId.toString());
                },
                RedisExecutor.ReadType.REFRESH_TOKEN,
                "RefreshToken 검증: userId=" + userId
        );
    }
    
    /**
     * Refresh Token 유효성 검증 Fallback (Circuit Breaker Open 상태)
     */
    private boolean isRefreshTokenValidFallback(String token, Long userId, Exception e) {
        return false; // Fail-Close
    }

    /**
     * Refresh Token 조회
     * 
     * 읽기 작업이지만 Refresh Token은 보안상 중요하므로 장애 시 null 반환
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "getRefreshTokenFallback")
    public Long getRefreshToken(String token) {
        return redisExecutor.executeRead(
                () -> {
                    String key = RedisKeyFactory.refreshToken(token);
                    String userIdStr = redisTemplate.opsForValue().get(key);
                    
                    if (userIdStr == null) {
                        return null;
                    }
                    
                    try {
                        return Long.valueOf(userIdStr);
                    } catch (NumberFormatException e) {
                        log.error("Refresh Token userId 파싱 실패: token={}", 
                                SensitiveDataMasker.maskToken(token), e);
                        return null;
                    }
                },
                RedisExecutor.ReadType.NULL_LONG,
                "RefreshToken 조회: " + SensitiveDataMasker.maskToken(token)
        );
    }
    
    /**
     * Refresh Token 조회 Fallback (Circuit Breaker Open 상태)
     */
    private Long getRefreshTokenFallback(String token, Exception e) {
        return null; // Fail-Close
    }

    /**
     * Refresh Token 삭제
     * 
     * 쓰기 작업이지만, 삭제 실패는 치명적이지 않으므로 장애 시에도 예외를 던지지 않음
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "deleteRefreshTokenFallback")
    public void deleteRefreshToken(String token, Long userId) {
        redisExecutor.executeDelete(
                () -> {
                    String key = RedisKeyFactory.refreshToken(token);
                    String metaKey = key + ":meta";
                    String userKey = RedisKeyFactory.refreshTokenSet(userId);
                    
                    // 토큰 키 삭제
                    redisTemplate.delete(key);
                    // 메타데이터 키 삭제
                    redisTemplate.delete(metaKey);
                    // 사용자별 Set에서 토큰 제거
                    redisTemplate.opsForSet().remove(userKey, token);
                    
                    log.debug("Refresh Token 삭제 완료: userId={}", userId);
                },
                "RefreshToken 삭제: userId=" + userId
        );
    }
    
    /**
     * Refresh Token 삭제 Fallback (Circuit Breaker Open 상태)
     */
    private void deleteRefreshTokenFallback(String token, Long userId, Exception e) {
        // 삭제 실패는 치명적이지 않으므로 예외를 던지지 않음
    }

    /**
     * 사용자별 Refresh Token 목록 조회
     * 
     * 읽기 작업이므로 장애 시 빈 Set 반환
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "getAllRefreshTokensByUserFallback")
    public Set<String> getAllRefreshTokensByUser(Long userId) {
        return redisExecutor.executeRead(
                () -> {
                    String userKey = RedisKeyFactory.refreshTokenSet(userId);
                    Set<String> tokens = redisTemplate.opsForSet().members(userKey);
                    return tokens != null ? tokens : Set.of();
                },
                RedisExecutor.ReadType.SET,
                "RefreshToken 목록 조회: userId=" + userId
        );
    }
    
    /**
     * 사용자별 Refresh Token 목록 조회 Fallback (Circuit Breaker Open 상태)
     */
    private Set<String> getAllRefreshTokensByUserFallback(Long userId, Exception e) {
        return Set.of(); // Fail-Open
    }

    // ==================== OAuth2 임시 토큰 관리 ====================

    /**
     * OAuth2 임시 토큰 저장
     * 
     * 쓰기 작업이므로 Redis 장애 시 예외 발생
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "saveTempTokenFallback")
    public void saveTempToken(String key, String accessToken, String refreshToken, String state,
                              String provider, String providerId, Duration ttl) {
        redisExecutor.executeWrite(
                () -> {
                    String redisKey = "oauth:temp:" + key;
                    Map<String, String> tokenData = new HashMap<>();
                    tokenData.put(Constant.ACCESS_TOKEN_KEY, accessToken != null ? accessToken : "");
                    tokenData.put(Constant.REFRESH_TOKEN_KEY, refreshToken != null ? refreshToken : "");
                    tokenData.put(Constant.STATE_KEY, state != null ? state : "");
                    tokenData.put(Constant.PROVIDER_KEY, provider != null ? provider : "");
                    tokenData.put(Constant.PROVIDER_ID_KEY, providerId != null ? providerId : "");
                    
                    redisTemplate.opsForHash().putAll(redisKey, tokenData);
                    redisTemplate.expire(redisKey, ttl);
                    
                    log.debug("OAuth2 임시 토큰 저장 완료: key={}", key);
                },
                "OAuth2 임시 토큰 저장: key=" + key
        );
    }
    
    /**
     * OAuth2 임시 토큰 저장 Fallback (Circuit Breaker Open 상태)
     */
    private void saveTempTokenFallback(String key, String accessToken, String refreshToken, 
                                       String state, String provider, String providerId, 
                                       Duration ttl, Exception e) {
        log.error("Circuit Breaker Open: OAuth2 임시 토큰 저장 실패 (Redis 장애) - key={}", key, e);
        throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Redis 장애로 인해 OAuth2 토큰 저장에 실패했습니다.");
    }

    /**
     * OAuth2 임시 토큰 조회 및 삭제
     * 
     * 읽기+쓰기 작업이지만, 조회 실패 시 빈 Map 반환
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "getAndDeleteTempTokenFallback")
    public Map<String, String> getAndDeleteTempToken(String key) {
        return redisExecutor.executeRead(
                () -> {
                    String redisKey = "oauth:temp:" + key;
                    
                    // Hash 조회
                    Map<Object, Object> rawData = redisTemplate.opsForHash().entries(redisKey);
                    
                    if (rawData == null || rawData.isEmpty()) {
                        return Map.of();
                    }
                    
                    // Object -> String 변환
                    Map<String, String> tokenData = new HashMap<>();
                    rawData.forEach((k, v) -> tokenData.put(String.valueOf(k), String.valueOf(v)));
                    
                    // 조회 후 삭제
                    redisTemplate.delete(redisKey);
                    
                    log.debug("OAuth2 임시 토큰 조회 및 삭제 완료: key={}", key);
                    return tokenData;
                },
                RedisExecutor.ReadType.MAP,
                "OAuth2 임시 토큰 조회/삭제: key=" + key
        );
    }
    
    /**
     * OAuth2 임시 토큰 조회 및 삭제 Fallback (Circuit Breaker Open 상태)
     */
    private Map<String, String> getAndDeleteTempTokenFallback(String key, Exception e) {
        return Map.of(); // Fail-Open
    }
}






