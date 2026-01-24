package org.example.sharedprompts.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.jwt.config.TokenTtlProperties;
import org.example.sharedprompts.auth.storage.RedisKeyFactory;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.dao.DataAccessException;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRedisServiceImpl implements TokenRedisService {

    private final StringRedisTemplate redisTemplate;
    private final TokenTtlProperties ttlProperties;

    // ==================== Access Token 관리 ====================

    @Override
    public void saveAccessToken(String token, Long userId) {
        try {
            String key = RedisKeyFactory.accessToken(token);
            Duration ttl = Duration.ofMillis(ttlProperties.getAccessTokenValidityMillis());
            
            redisTemplate.opsForValue().set(key, String.valueOf(userId), ttl);
            log.debug("Access Token 저장 완료: userId={}", userId);
        } catch (DataAccessException e) {
            log.error("Access Token 저장 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean isAccessTokenValid(String token) {
        try {
            String key = RedisKeyFactory.accessToken(token);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (DataAccessException e) {
            log.error("Access Token 유효성 검증 실패: token={}", SensitiveDataMasker.maskToken(token), e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteAccessToken(String token) {
        try {
            String key = RedisKeyFactory.accessToken(token);
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.FALSE.equals(deleted)) {
                log.debug("삭제할 Access Token이 없음: token={}", SensitiveDataMasker.maskToken(token));
            }
        } catch (DataAccessException e) {
            log.error("Access Token 삭제 실패: token={}", SensitiveDataMasker.maskToken(token), e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean isAccessTokenValidWithUserId(String accessToken, Long userId) {
        try {
            String key = RedisKeyFactory.accessToken(accessToken);
            String storedUserId = redisTemplate.opsForValue().get(key);
            return storedUserId != null && storedUserId.equals(userId.toString());
        } catch (DataAccessException e) {
            log.error("Access Token 유효성 검증 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== Refresh Token 관리 ====================

    @Override
    public boolean isRefreshTokenValid(String token, Long userId) {
        try {
            String key = RedisKeyFactory.refreshToken(token);
            String storedUserId = redisTemplate.opsForValue().get(key);
            return storedUserId != null && storedUserId.equals(userId.toString());
        } catch (DataAccessException e) {
            log.error("Refresh Token 유효성 검증 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Long getRefreshToken(String token) {
        try {
            String key = RedisKeyFactory.refreshToken(token);
            String userIdStr = redisTemplate.opsForValue().get(key);
            
            if (userIdStr == null) {
                return null;
            }
            
            return Long.valueOf(userIdStr);
        } catch (DataAccessException e) {
            log.error("Refresh Token 조회 실패: token={}", SensitiveDataMasker.maskToken(token), e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (NumberFormatException e) {
            log.error("Refresh Token userId 파싱 실패: token={}", SensitiveDataMasker.maskToken(token), e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteRefreshToken(String token, Long userId) {
        try {
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
        } catch (DataAccessException e) {
            log.error("Refresh Token 삭제 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Set<String> getAllRefreshTokensByUser(Long userId) {
        try {
            String userKey = RedisKeyFactory.refreshTokenSet(userId);
            Set<String> tokens = redisTemplate.opsForSet().members(userKey);
            return tokens != null ? tokens : Set.of();
        } catch (DataAccessException e) {
            log.error("Refresh Token 목록 조회 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== OAuth2 임시 토큰 관리 ====================

    @Override
    public void saveTempToken(String key, String accessToken, String refreshToken, String state,
                              String provider, String providerId, Duration ttl) {
        try {
            String redisKey = "oauth:temp:" + key;
            Map<String, String> tokenData = new HashMap<>();
            tokenData.put("accessToken", accessToken != null ? accessToken : "");
            tokenData.put("refreshToken", refreshToken != null ? refreshToken : "");
            tokenData.put("state", state != null ? state : "");
            tokenData.put("provider", provider != null ? provider : "");
            tokenData.put("providerId", providerId != null ? providerId : "");
            
            redisTemplate.opsForHash().putAll(redisKey, tokenData);
            redisTemplate.expire(redisKey, ttl);
            
            log.debug("OAuth2 임시 토큰 저장 완료: key={}", key);
        } catch (DataAccessException e) {
            log.error("OAuth2 임시 토큰 저장 실패: key={}", key, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Map<String, String> getAndDeleteTempToken(String key) {
        try {
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
        } catch (DataAccessException e) {
            log.error("OAuth2 임시 토큰 조회/삭제 실패: key={}", key, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}

