package org.example.sharedprompts.global.redis;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.jwt.TokenTtlProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TokenRedisService {

    private final StringRedisTemplate redisTemplate;
    private final TokenTtlProperties ttlConfig ;

    private static final String ACCESS_PREFIX = "ACCESS:";
    private static final String REFRESH_SET_PREFIX = "USER_REFRESH:";

    // =========================
    // Access/Refresh Token 관리
    // =========================
    public void saveAccessToken(String token, Long userId) {
        redisTemplate.opsForValue().set(
                ACCESS_PREFIX + token,
                userId.toString(),
                Duration.ofMillis(ttlConfig.getAccessTokenValidity())
        );
    }

    public void saveRefreshToken(String token, Long userId) {
        String key = REFRESH_SET_PREFIX + userId;
        redisTemplate.opsForSet().add(key, token);
        redisTemplate.expire(key, Duration.ofMillis(ttlConfig.getRefreshTokenValidity()));
    }

    public boolean isAccessTokenValid(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ACCESS_PREFIX + token));
    }

    public boolean isRefreshTokenValid(String token, Long userId) {
        String key = REFRESH_SET_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, token));
    }

    public void deleteRefreshToken(String token, Long userId) {
        String key = REFRESH_SET_PREFIX + userId;
        redisTemplate.opsForSet().remove(key, token);
    }

    public void deleteAccessToken(String token) {
        redisTemplate.delete(ACCESS_PREFIX + token);
    }

    // =========================
    // OAuth2 임시 토큰 관리 (access, refresh, state)
    // =========================
    public void saveTempToken(String key, String accessToken, String refreshToken, String state, Duration ttl) {
        Map<String, String> tokens = Map.of(
                "access_token", accessToken,
                "refresh_token", refreshToken,
                "state", state
        );
        redisTemplate.opsForHash().putAll(key, tokens);
        redisTemplate.expire(key, ttl);
    }

    public Map<String, String> getAndDeleteTempToken(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) return null;

        redisTemplate.delete(key); // 1회성 사용
        return entries.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> (String)e.getKey(),
                        e -> (String)e.getValue()
                ));
    }
}
