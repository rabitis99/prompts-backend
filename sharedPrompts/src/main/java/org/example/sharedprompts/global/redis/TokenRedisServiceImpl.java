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
public class TokenRedisServiceImpl implements TokenRedisService {

    private final StringRedisTemplate redisTemplate;
    private final TokenTtlProperties ttlConfig;

    private static final String ACCESS_PREFIX = "ACCESS:";
    private static final String REFRESH_PREFIX = "REFRESH:";
    private static final String REFRESH_SET_PREFIX = "USER_REFRESH:";

    // =========================
    // 🔹 Access Token 관리
    // =========================

    @Override
    public void saveAccessToken(String token, Long userId) {
        redisTemplate.opsForValue().set(
                ACCESS_PREFIX + token,
                String.valueOf(userId),
                Duration.ofMinutes(ttlConfig.getAccessTokenValidity())
        );
    }

    @Override
    public boolean isAccessTokenValid(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ACCESS_PREFIX + token));
    }

    @Override
    public void deleteAccessToken(String token) {
        redisTemplate.delete(ACCESS_PREFIX + token);
    }

    @Override
    public boolean isAccessTokenValidWithUserId(String accessToken, Long userId) {
        String key = ACCESS_PREFIX + accessToken;
        String storedUserIdStr = redisTemplate.opsForValue().get(key);

        if (storedUserIdStr == null) return false;

        try {
            Long savedUserId = Long.valueOf(storedUserIdStr);
            return userId.equals(savedUserId);
        } catch (NumberFormatException e) {
            redisTemplate.delete(key);
            return false;
        }
    }

    // =========================
    // 🔹 Refresh Token 관리 (개별 키 + 사용자별 Set)
    // =========================

    @Override
    public void saveRefreshToken(String token, Long userId) {
        String key = REFRESH_PREFIX + token;
        redisTemplate.opsForValue().set(
                key,
                String.valueOf(userId),
                Duration.ofMinutes(ttlConfig.getRefreshTokenValidity())
        );

        String userKey = REFRESH_SET_PREFIX + userId;
        // Only set TTL if the Set doesn't exist yet (first token for this user)
        // This avoids unnecessary TTL reset on every token save
        // Individual tokens have their own TTL, so the Set TTL is mainly for cleanup
        boolean setExists = Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
        redisTemplate.opsForSet().add(userKey, token);
        if (!setExists) {
            redisTemplate.expire(userKey, Duration.ofMinutes(ttlConfig.getRefreshTokenValidity()));
        }
    }

    @Override
    public boolean isRefreshTokenValid(String token, Long userId) {
        String key = REFRESH_PREFIX + token;
        String storedUserId = redisTemplate.opsForValue().get(key);
        return storedUserId != null && storedUserId.equals(userId.toString());
    }

    @Override
    public Long getRefreshToken(String token) {
        String key = REFRESH_PREFIX + token;
        String storedUserId = redisTemplate.opsForValue().get(key);
        if (storedUserId == null) return null;
        try{
            return Long.valueOf(storedUserId);
        } catch (NumberFormatException e) {
            redisTemplate.delete(key);
            return null;
        }
    }


    @Override
    public void deleteRefreshToken(String token, Long userId) {
        redisTemplate.delete(REFRESH_PREFIX + token);

        String userKey = REFRESH_SET_PREFIX + userId;
        redisTemplate.opsForSet().remove(userKey, token);
    }

    @Override
    public Set<String> getAllRefreshTokensByUser(Long userId) {
        String userKey = REFRESH_SET_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userKey);
        return tokens != null ? tokens : Set.of();
    }

    // =========================
    // 🔹 OAuth2 임시 토큰 관리 (access, refresh, state)
    // =========================
    @Override
    public void saveTempToken(String key, String accessToken, String refreshToken, String state,
                              String provider, String providerId, Duration ttl) {
        Map<String, String> tokens = Map.of(
                Constant.ACCESS_TOKEN_KEY, accessToken,
                Constant.REFRESH_TOKEN_KEY, refreshToken,
                Constant.STATE_KEY, state,
                Constant.PROVIDER_KEY, provider,
                Constant.PROVIDER_ID_KEY, providerId
        );
        redisTemplate.opsForHash().putAll(key, tokens);
        redisTemplate.expire(key, ttl);
    }

    @Override
    public Map<String, String> getAndDeleteTempToken(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) return null;

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

        redisTemplate.delete(key);
        return result;
    }

}

