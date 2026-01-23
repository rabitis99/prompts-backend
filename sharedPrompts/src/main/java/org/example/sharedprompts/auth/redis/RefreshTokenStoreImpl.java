package org.example.sharedprompts.auth.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.jwt.config.TokenTtlProperties;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 저장소 구현체
 * 
 * Redis 기반으로 Refresh Token을 저장하고 관리합니다.
 * 1회용 보장을 위해 getAndDelete 메서드를 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenStoreImpl implements RefreshTokenStore {

    private final StringRedisTemplate redisTemplate;
    private final TokenTtlProperties ttlProperties;

    @Override
    public void save(String token, Long userId, String ip, String userAgent) {
        try {
            String key = RedisKeyFactory.refreshToken(token);
            long ttlMillis = ttlProperties.getRefreshTokenValidityMillis();
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
            String userKey = RedisKeyFactory.refreshTokenSet(userId);
            boolean setExists = Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
            redisTemplate.opsForSet().add(userKey, token);
            if (!setExists) {
                redisTemplate.expire(userKey, ttl);
            }
        } catch (DataAccessException e) {
            log.error("Refresh Token 저장 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public RefreshTokenMetadata getAndDelete(String token) {
        try {
            String key = RedisKeyFactory.refreshToken(token);
            String userIdStr = redisTemplate.opsForValue().get(key);
            
            if (userIdStr == null) {
                return null; // 토큰이 없거나 이미 사용됨
            }
            
            // Hash에서 메타데이터 조회
            String metaKey = key + ":meta";
            Map<Object, Object> metadata = redisTemplate.opsForHash().entries(metaKey);
            String ip = (String) metadata.get("ip");
            String userAgent = (String) metadata.get("userAgent");
            
            // 남은 TTL 계산
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            long remainingTtlMillis = ttl != null && ttl > 0 ? ttl : 0L;
            
            // 즉시 삭제 (1회용 보장)
            redisTemplate.delete(key);
            redisTemplate.delete(metaKey);
            
            // Set에서도 제거
            Long userId = Long.valueOf(userIdStr);
            String userKey = RedisKeyFactory.refreshTokenSet(userId);
            redisTemplate.opsForSet().remove(userKey, token);
            
            return new RefreshTokenMetadata(userId, ip, userAgent, remainingTtlMillis);
        } catch (DataAccessException e) {
            log.error("Refresh Token 조회/삭제 실패: token={}", token, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean isValid(String token, Long userId) {
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
    public void delete(String token, Long userId) {
        try {
            String key = RedisKeyFactory.refreshToken(token);
            redisTemplate.delete(key);
            redisTemplate.delete(key + ":meta");
            
            String userKey = RedisKeyFactory.refreshTokenSet(userId);
            redisTemplate.opsForSet().remove(userKey, token);
        } catch (DataAccessException e) {
            log.error("Refresh Token 삭제 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Set<String> getAllByUser(Long userId) {
        try {
            String userKey = RedisKeyFactory.refreshTokenSet(userId);
            Set<String> tokens = redisTemplate.opsForSet().members(userKey);
            return tokens != null ? tokens : Set.of();
        } catch (DataAccessException e) {
            log.error("Refresh Token 목록 조회 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteAllByUser(Long userId) {
        try {
            String userKey = RedisKeyFactory.refreshTokenSet(userId);
            Set<String> tokens = getAllByUser(userId);
            
            for (String token : tokens) {
                String key = RedisKeyFactory.refreshToken(token);
                redisTemplate.delete(key);
                redisTemplate.delete(key + ":meta");
            }
            
            redisTemplate.delete(userKey);
        } catch (DataAccessException e) {
            log.error("사용자별 Refresh Token 전체 삭제 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void cleanupExpiredTokens(Long userId) {
        try {
            String userKey = RedisKeyFactory.refreshTokenSet(userId);
            Set<String> tokens = getAllByUser(userId);
            
            if (tokens == null || tokens.isEmpty()) {
                return;
            }
            
            for (String token : tokens) {
                String key = RedisKeyFactory.refreshToken(token);
                if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                    // 만료된 토큰 제거
                    redisTemplate.opsForSet().remove(userKey, token);
                    log.debug("만료된 Refresh Token 정리: userId={}, token={}", userId, token);
                }
            }
        } catch (DataAccessException e) {
            log.error("만료된 Refresh Token 정리 실패: userId={}", userId, e);
            // 정리 작업 실패는 치명적이지 않으므로 예외를 던지지 않음
        }
    }
}

