package org.example.sharedprompts.global.redis;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.jwt.config.TokenTtlProperties;
import org.example.sharedprompts.auth.resilience.RedisExecutor;
import org.example.sharedprompts.auth.storage.RedisKeyFactory;
import org.example.sharedprompts.global.Lua.LuaScripts;
import org.example.sharedprompts.global.constant.Constant;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    private DefaultRedisScript<List<String>> getAndDeleteTempTokenScript;
    
    @PostConstruct
    public void init() {
        // GET_AND_DELETE_TEMP_TOKEN 스크립트 초기화
        DefaultRedisScript<List<String>> getAndDelete = new DefaultRedisScript<>();
        getAndDelete.setScriptText(LuaScripts.GET_AND_DELETE_TEMP_TOKEN);
        // Spring Data Redis는 런타임에 제네릭 타입 정보를 잃어버리므로 raw type을 사용
        @SuppressWarnings("unchecked")
        Class<List<String>> resultType = (Class<List<String>>) (Class<?>) List.class;
        getAndDelete.setResultType(resultType);
        this.getAndDeleteTempTokenScript = getAndDelete;
    }

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
                    String redisKey = RedisKeyFactory.oauthTemp(key);
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
     * 읽기+쓰기 작업이므로 executeReadWrite()를 사용합니다.
     * Lua 스크립트가 DEL 명령을 실행하므로 master에 연결되어야 합니다.
     * TOCTOU 문제를 방지하기 위해 조회와 삭제를 원자적으로 처리합니다.
     */
    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "getAndDeleteTempTokenFallback")
    public Map<String, String> getAndDeleteTempToken(String key) {
        Map<String, String> result = redisExecutor.executeReadWrite(
                () -> {
                    String redisKey = RedisKeyFactory.oauthTemp(key);
                    
                    // Lua 스크립트를 사용하여 원자적으로 조회 및 삭제
                    // 스크립트는 평탄화된 배열 [field1, value1, field2, value2, ...]을 반환
                    List<String> entries = redisTemplate.execute(
                            getAndDeleteTempTokenScript,
                            List.of(redisKey)
                    );
                    
                    if (entries == null || entries.isEmpty()) {
                        return Map.of(); // 토큰이 없거나 이미 사용됨
                    }
                    
                    // 평탄화된 배열을 Map으로 변환
                    Map<String, String> tokenData = new HashMap<>();
                    for (int i = 0; i < entries.size(); i += 2) {
                        if (i + 1 < entries.size()) {
                            tokenData.put(entries.get(i), entries.get(i + 1));
                        }
                    }
                    
                    log.debug("OAuth2 임시 토큰 조회 및 삭제 완료: key={}", key);
                    return tokenData;
                },
                "OAuth2 임시 토큰 조회/삭제: key=" + key
        );
        
        // executeReadWrite는 장애 시 null을 반환할 수 있으므로 빈 Map으로 변환
        return result != null ? result : Map.of();
    }
    
    /**
     * OAuth2 임시 토큰 조회 및 삭제 Fallback (Circuit Breaker Open 상태)
     */
    private Map<String, String> getAndDeleteTempTokenFallback(String key, Exception e) {
        return Map.of(); // Fail-Open
    }
}