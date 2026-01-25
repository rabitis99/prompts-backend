package org.example.sharedprompts.auth.storage;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.jwt.config.TokenTtlProperties;
import org.example.sharedprompts.auth.resilience.RedisExecutor;
import org.example.sharedprompts.global.Lua.LuaScripts;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Refresh Token 저장소 구현체
 * 
 * Redis 기반으로 Refresh Token을 저장하고 관리합니다.
 * 1회용 보장을 위해 getAndDelete 메서드를 제공합니다.
 * 
 * Redis 장애 대응:
 * - Circuit Breaker 패턴 적용으로 장애 격리
 * - 쓰기 작업(토큰 저장): Redis 장애 시 예외 발생 (데이터 일관성 보장)
 * - 읽기 작업(토큰 조회/검증): Refresh Token은 보안상 중요하므로 장애 시 null/false 반환 (Fail-Close)
 * - Redis Health Check 연동으로 자동 Fail-Open 정책 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenStoreImpl implements RefreshTokenStore {

    private final StringRedisTemplate redisTemplate;
    private final TokenTtlProperties ttlProperties;
    private final RedisExecutor redisExecutor;
    
    private DefaultRedisScript<List<String>> getAndDeleteScript;
    private DefaultRedisScript<Long> deleteScript;
    private DefaultRedisScript<Long> deleteAllByUserScript;
    
    @PostConstruct
    public void init() {
        // GET_AND_DELETE 스크립트 초기화
        DefaultRedisScript<List<String>> getAndDelete = new DefaultRedisScript<>();
        getAndDelete.setScriptText(LuaScripts.GET_AND_DELETE_REFRESH_TOKEN);
        // Spring Data Redis는 런타임에 제네릭 타입 정보를 잃어버리므로 raw type을 사용
        @SuppressWarnings("unchecked")
        Class<List<String>> resultType = (Class<List<String>>) (Class<?>) List.class;
        getAndDelete.setResultType(resultType);
        this.getAndDeleteScript = getAndDelete;
        
        // DELETE 스크립트 초기화
        DefaultRedisScript<Long> delete = new DefaultRedisScript<>();
        delete.setScriptText(LuaScripts.DELETE_REFRESH_TOKEN);
        delete.setResultType(Long.class);
        this.deleteScript = delete;
        
        // DELETE_ALL_BY_USER 스크립트 초기화
        DefaultRedisScript<Long> deleteAll = new DefaultRedisScript<>();
        deleteAll.setScriptText(LuaScripts.DELETE_ALL_REFRESH_TOKENS_BY_USER);
        deleteAll.setResultType(Long.class);
        this.deleteAllByUserScript = deleteAll;
    }

    @Override
    public void save(String token, Long userId, String ip, String userAgent) {
        long ttlMillis = ttlProperties.getRefreshTokenValidityMillis();
        saveWithTtl(token, userId, ip, userAgent, ttlMillis);
    }
    
    /**
     * Refresh Token 저장 (TTL 지정)
     * 
     * 쓰기 작업이므로 Redis 장애 시 예외 발생 (데이터 일관성 보장)
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "saveWithTtlFallback")
    public void saveWithTtl(String token, Long userId, String ip, String userAgent, long ttlMillis) {
        redisExecutor.executeWrite(
                () -> {
                    String key = RedisKeyFactory.refreshToken(token);
                    Duration ttl = Duration.ofMillis(ttlMillis);
                    
                    // 토큰 저장
                    redisTemplate.opsForValue().set(
                            key,
                            String.valueOf(userId),
                            ttl
                    );
                    
                    // 메타데이터 저장 (Hash)
                    String metaKey = key + ":meta";
                    Map<String, String> metadata = Map.of(
                            "ip", ip != null ? ip : "",
                            "userAgent", userAgent != null ? userAgent : ""
                    );
                    redisTemplate.opsForHash().putAll(metaKey, metadata);
                    redisTemplate.expire(metaKey, ttl);
                    
                    // 사용자별 Set에 추가
                    // TOCTOU 경쟁 조건 방지: add 후 항상 expire 호출
                    String userKey = RedisKeyFactory.refreshTokenSet(userId);
                    redisTemplate.opsForSet().add(userKey, token);
                    redisTemplate.expire(userKey, ttl);
                },
                "RefreshToken 저장: userId=" + userId
        );
    }
    
    /**
     * Refresh Token 저장 Fallback (Circuit Breaker Open 상태)
     */
    private void saveWithTtlFallback(String token, Long userId, String ip, String userAgent, 
                                     long ttlMillis, Exception e) {
        log.error("Circuit Breaker Open: Refresh Token 저장 실패 (Redis 장애) - userId={}, token={}", 
                userId, SensitiveDataMasker.maskToken(token), e);
        throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Redis 장애로 인해 Refresh Token 저장에 실패했습니다.");
    }

    /**
     * Refresh Token 조회 및 삭제 (1회용 보장)
     * 
     * 읽기+쓰기 작업이므로 executeReadWrite()를 사용합니다.
     * Lua 스크립트가 DEL/SREM 명령을 실행하므로 master에 연결되어야 합니다.
     * Refresh Token은 보안상 중요하므로 장애 시 null 반환 (Fail-Close)
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "getAndDeleteFallback")
    public RefreshTokenMetadata getAndDelete(String token) {
        return redisExecutor.executeReadWrite(
                () -> {
                    String key = RedisKeyFactory.refreshToken(token);
                    String metaKey = key + ":meta";
                    String setKeyPrefix = RedisKeyFactory.getRefreshTokenSetPrefix();
                    
                    // Lua 스크립트를 사용하여 원자적으로 조회 및 삭제
                    // 스크립트 내부에서 userId를 조회한 후 userSetKey를 동적으로 구성
                    // 스크립트는 DEL/SREM 명령을 실행하므로 쓰기 작업입니다.
                    List<String> result = redisTemplate.execute(
                            getAndDeleteScript,
                            List.of(key, metaKey),
                            token,
                            setKeyPrefix
                    );
                    
                    if (result == null || result.isEmpty()) {
                        return null; // 토큰이 없거나 이미 사용됨
                    }
                    
                    // 결과 파싱: [userId, ip, userAgent, remainingTtlMillis]
                    String userIdStr = result.get(0);
                    String ip = result.size() > 1 ? result.get(1) : "";
                    String userAgent = result.size() > 2 ? result.get(2) : "";
                    long remainingTtlMillis = 0L;
                     if (result.size() > 3) {
                         try {
                             remainingTtlMillis = Long.parseLong(result.get(3));
                            } catch (NumberFormatException e) {
                            log.warn("Refresh Token TTL 파싱 실패: token={}",
                                    SensitiveDataMasker.maskToken(token), e);
                         }
                     }
                     
                    try {
                        Long userId = Long.valueOf(userIdStr);
                        return new RefreshTokenMetadata(userId, ip, userAgent, remainingTtlMillis);
                    } catch (NumberFormatException e) {
                        log.error("Refresh Token 메타데이터 파싱 실패: token={}", 
                                SensitiveDataMasker.maskToken(token), e);
                        return null;
                    }
                },
                "RefreshToken 조회/삭제: " + SensitiveDataMasker.maskToken(token)
        );
    }
    
    /**
     * Refresh Token 조회 및 삭제 Fallback (Circuit Breaker Open 상태)
     */
    private RefreshTokenMetadata getAndDeleteFallback(String token, Exception e) {
        return null; // Fail-Close
    }

    /**
     * Refresh Token 유효성 검증
     * 
     * 읽기 작업이지만 Refresh Token은 보안상 중요하므로 장애 시 false 반환 (Fail-Close)
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "isValidFallback")
    public boolean isValid(String token, Long userId) {
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
    private boolean isValidFallback(String token, Long userId, Exception e) {
        return false; // Fail-Close
    }

    /**
     * Refresh Token 삭제
     * 
     * 쓰기 작업이지만, 삭제 실패는 치명적이지 않으므로 장애 시에도 예외를 던지지 않음
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "deleteFallback")
    public void delete(String token, Long userId) {
        redisExecutor.executeDelete(
                () -> {
                    String key = RedisKeyFactory.refreshToken(token);
                    String metaKey = key + ":meta";
                    String userKey = RedisKeyFactory.refreshTokenSet(userId);
                    
                    // Lua 스크립트를 사용하여 원자적으로 삭제
                    Long deletedCount = redisTemplate.execute(
                            deleteScript,
                            List.of(key, metaKey, userKey),
                            token
                    );
                    
                    if (deletedCount == null || deletedCount == 0) {
                        log.debug("삭제할 Refresh Token이 없음: token={}, userId={}", 
                                SensitiveDataMasker.maskToken(token), userId);
                    }
                },
                "RefreshToken 삭제: userId=" + userId
        );
    }
    
    /**
     * Refresh Token 삭제 Fallback (Circuit Breaker Open 상태)
     */
    private void deleteFallback(String token, Long userId, Exception e) {
        // 삭제 실패는 치명적이지 않으므로 예외를 던지지 않음
    }

    /**
     * 사용자별 Refresh Token 목록 조회
     * 
     * 읽기 작업이므로 장애 시 빈 Set 반환
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "getAllByUserFallback")
    public Set<String> getAllByUser(Long userId) {
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
    private Set<String> getAllByUserFallback(Long userId, Exception e) {
        return Set.of(); // Fail-Open
    }

    /**
     * 사용자별 Refresh Token 전체 삭제
     * 
     * 쓰기 작업이지만, 삭제 실패는 치명적이지 않으므로 장애 시에도 예외를 던지지 않음
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "deleteAllByUserFallback")
    public void deleteAllByUser(Long userId) {
        redisExecutor.executeDelete(
                () -> {
                    String userKey = RedisKeyFactory.refreshTokenSet(userId);
                    String tokenPrefix = RedisKeyFactory.getRefreshTokenPrefix();
                    
                    // Lua 스크립트를 사용하여 원자적으로 모든 토큰 삭제
                    Long deletedCount = redisTemplate.execute(
                            deleteAllByUserScript,
                            List.of(userKey),
                            tokenPrefix
                    );
                    
                    if (deletedCount == null) {
                        deletedCount = 0L;
                    }
                    log.debug("사용자별 Refresh Token 전체 삭제 완료: userId={}, deletedCount={}", userId, deletedCount);
                },
                "RefreshToken 전체 삭제: userId=" + userId
        );
    }
    
    /**
     * 사용자별 Refresh Token 전체 삭제 Fallback (Circuit Breaker Open 상태)
     */
    private void deleteAllByUserFallback(Long userId, Exception e) {
        // 삭제 실패는 치명적이지 않으므로 예외를 던지지 않음
    }

    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "cleanupExpiredTokensFallback")
    public void cleanupExpiredTokens(Long userId) {
        redisExecutor.executeDelete(
                () -> {
                    String userKey = RedisKeyFactory.refreshTokenSet(userId);
                    Set<String> tokens = getAllByUser(userId);

                    if (tokens == null || tokens.isEmpty()) {
                        return;
                    }

                    for (String token : tokens) {
                        String key = RedisKeyFactory.refreshToken(token);
                        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                            redisTemplate.opsForSet().remove(userKey, token);
                            log.debug("만료된 Refresh Token 정리: userId={}", userId);
                        }
                    }
                },
                "만료된 RefreshToken 정리: userId=" + userId
        );
    }

    private void cleanupExpiredTokensFallback(Long userId, Throwable e) {
        // 정리 작업 실패는 치명적이지 않으므로 예외를 전파하지 않음
        log.warn("cleanupExpiredTokens fallback 실행: userId={}", userId, e);
    }
}

