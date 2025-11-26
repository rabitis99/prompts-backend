package org.example.sharedprompts.global.redis;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.constant.Constant;
import org.example.sharedprompts.global.jwt.TokenTtlProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TokenRedisService {

    private final StringRedisTemplate redisTemplate;
    private final TokenTtlProperties ttlConfig;

    private static final String ACCESS_PREFIX = "ACCESS:";
    private static final String REFRESH_PREFIX = "REFRESH:";
    private static final String REFRESH_SET_PREFIX = "USER_REFRESH:";

    // =========================
    // Access Token 관리
    // =========================
    public void saveAccessToken(String token, Long userId) {
        redisTemplate.opsForValue().set(
                ACCESS_PREFIX + token,
                userId.toString(),
                Duration.ofMillis(ttlConfig.getAccessTokenValidity())
        );
    }

    public boolean isAccessTokenValid(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ACCESS_PREFIX + token));
    }

    public void deleteAccessToken(String token) {
        redisTemplate.delete(ACCESS_PREFIX + token);
    }

    // =========================
    // Refresh Token 관리 (개별 키 + 사용자별 Set)
    // =========================
    public void saveRefreshToken(String token, Long userId) {
        // 1) 개별 키 저장
        String key = REFRESH_PREFIX + token;
        redisTemplate.opsForValue().set(
                key,
                userId.toString(),
                Duration.ofMillis(ttlConfig.getRefreshTokenValidity())
        );

        // 2) 사용자별 Refresh Token Set 관리
        String userKey = REFRESH_SET_PREFIX + userId;
        redisTemplate.opsForSet().add(userKey, token);
        redisTemplate.expire(userKey, Duration.ofMillis(ttlConfig.getRefreshTokenValidity()));
    }

    public boolean isRefreshTokenValid(String token, Long userId) {
        String key = REFRESH_PREFIX + token;
        String storedUserId = redisTemplate.opsForValue().get(key);
        return storedUserId != null && storedUserId.equals(userId.toString());
    }

    public void deleteRefreshToken(String token, Long userId) {
        // 개별 키 삭제
        redisTemplate.delete(REFRESH_PREFIX + token);

        // 사용자별 Set에서도 제거
        String userKey = REFRESH_SET_PREFIX + userId;
        redisTemplate.opsForSet().remove(userKey, token);
    }

    public Set<String> getAllRefreshTokensByUser(Long userId) {
        String userKey = REFRESH_SET_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userKey);
        return tokens != null ? tokens : Set.of();
    }

    // =========================
    // OAuth2 임시 토큰 관리 (access, refresh, state)
    // =========================
    public void saveTempToken(String key, String accessToken, String refreshToken, String state, Duration ttl) {
        Map<String, String> tokens = Map.of(
                Constant.ACCESS_TOKEN_KEY, accessToken,
                Constant.REFRESH_TOKEN_KEY, refreshToken,
                Constant.STATE_KEY, state
        );
        redisTemplate.opsForHash().putAll(key, tokens);
        redisTemplate.expire(key, ttl);
    }

    public Map<String, String> getAndDeleteTempToken(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) return null;

        // 먼저 타입 안전하게 변환
        Map<String, String> result = entries.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> {
                            if (!(e.getKey() instanceof String)) {
                                throw new IllegalStateException("Redis hash key is not a String: " + e.getKey());
                            }
                            return (String) e.getKey();
                        },
                        e -> {
                            if (!(e.getValue() instanceof String)) {
                                throw new IllegalStateException("Redis hash value is not a String: " + e.getValue());
                            }
                            return (String) e.getValue();
                        }
                ));

        // 변환 성공 후 안전하게 삭제
        redisTemplate.delete(key);
        return result;
    }
}
