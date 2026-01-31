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
    
    private final DefaultRedisScript<List<String>> getAndDeleteOAuth2TempSessionScript;

    // ==================== Access Token 관리 ====================

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
    
    private void saveAccessTokenFallback(String token, Long userId, Exception e) {
        log.error("Circuit Breaker Open: Access Token 저장 실패 (Redis 장애) - userId={}, token={}", 
                userId, SensitiveDataMasker.maskToken(token), e);
        throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Redis 장애로 인해 토큰 저장에 실패했습니다.");
    }

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
    
    private boolean isAccessTokenValidFallback(String token, Exception e) {
        return true; // Fail-Open
    }

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

    private void deleteAccessTokenFallback(String token, Exception e) {
    }

    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "saveOAuth2TempSessionFallback")
    public void saveOAuth2TempSession(String tempKey, String provider, String providerId, String state, Duration ttl) {
        redisExecutor.executeWrite(
                () -> {
                    String redisKey = RedisKeyFactory.oauthTemp(tempKey);
                    // 필수 필드 검증: provider와 providerId는 OAuth2 플로우에서 절대 null이 아니어야 함
                    if (provider == null || providerId == null) {
                        throw new IllegalArgumentException("provider와 providerId는 필수입니다.");
                    }
                    Map<String, String> sessionData = new HashMap<>();
                    sessionData.put(Constant.PROVIDER_KEY, provider);
                    sessionData.put(Constant.PROVIDER_ID_KEY, providerId);
                    sessionData.put(Constant.STATE_KEY, state != null ? state : "");
                    
                    redisTemplate.opsForHash().putAll(redisKey, sessionData);
                    redisTemplate.expire(redisKey, ttl);
                    
                    log.debug("OAuth2 임시 인증 세션 저장 완료: tempKey={}, provider={}", tempKey, provider);
                },
                "OAuth2 임시 인증 세션 저장: tempKey=" + tempKey
        );
    }
    
    private void saveOAuth2TempSessionFallback(String tempKey, String provider, String providerId, 
                                                String state, Duration ttl, Exception e) {
        log.error("Circuit Breaker Open: OAuth2 임시 인증 세션 저장 실패 (Redis 장애) - tempKey={}", tempKey, e);
        throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Redis 장애로 인해 OAuth2 임시 인증 세션 저장에 실패했습니다.");
    }

    @Override
    @CircuitBreaker(name = "tokenRedis", fallbackMethod = "getAndDeleteOAuth2TempSessionFallback")
    public Map<String, String> getAndDeleteOAuth2TempSession(String tempKey) {
        Map<String, String> result = redisExecutor.executeReadWrite(
                () -> {
                    String redisKey = RedisKeyFactory.oauthTemp(tempKey);
                    List<String> entries = redisTemplate.execute(
                            getAndDeleteOAuth2TempSessionScript,
                            List.of(redisKey)
                    );
                    
                    if (entries == null || entries.isEmpty()) {
                        return Map.of();
                    }
                    
                    Map<String, String> sessionData = new HashMap<>();
                    for (int i = 0; i < entries.size(); i += 2) {
                        if (i + 1 < entries.size()) {
                            sessionData.put(entries.get(i), entries.get(i + 1));
                        }
                    }
                    
                    log.debug("OAuth2 임시 인증 세션 조회 및 삭제 완료: tempKey={}", tempKey);
                    return sessionData;
                },
                "OAuth2 임시 인증 세션 조회/삭제: tempKey=" + tempKey
        );
        
        return result != null ? result : Map.of();
    }
    
    private Map<String, String> getAndDeleteOAuth2TempSessionFallback(String tempKey, Exception e) {
        return Map.of();
    }
}