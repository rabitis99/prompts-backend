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
    // 🔹 Access Token 관리
    // =========================

    /** Access Token 저장 (userId 포함) */
    public void saveAccessToken(String token, Long userId) {
        redisTemplate.opsForValue().set(
                ACCESS_PREFIX + token,
                String.valueOf(userId),
                Duration.ofMillis(ttlConfig.getAccessTokenValidity())
        );
    }

    /** Access Token 유효성 확인 */
    public boolean isAccessTokenValid(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ACCESS_PREFIX + token));
    }

    /** Access Token 삭제 (검증 없이) */
    public void deleteAccessToken(String token) {
        redisTemplate.delete(ACCESS_PREFIX + token);
    }

    /** Access Token 삭제 (userId 검증 포함) */
    public boolean isRefreshTokenValid(Long userId, String accessToken) {
        String key = ACCESS_PREFIX + accessToken;
        String storedUserIdStr = redisTemplate.opsForValue().get(key);

        if (storedUserIdStr == null) return false;

        Long storedUserId = Long.valueOf(storedUserIdStr);
        if (!storedUserId.equals(userId)) {
            return false;
        }

        redisTemplate.delete(key);
        return true;
    }

    // =========================
    // 🔹 Refresh Token 관리 (개별 키 + 사용자별 Set)
    // =========================

    public void saveRefreshToken(String token, Long userId) {
        String key = REFRESH_PREFIX + token;
        redisTemplate.opsForValue().set(
                key,
                userId.toString(),
                Duration.ofMillis(ttlConfig.getRefreshTokenValidity())
        );

        String userKey = REFRESH_SET_PREFIX + userId;
        redisTemplate.opsForSet().add(userKey, token);
        redisTemplate.expire(userKey, Duration.ofMillis(ttlConfig.getRefreshTokenValidity()));
    }

    public boolean isRefreshTokenValid(String token, Long userId) {
        String key = REFRESH_PREFIX + token;
        String storedUserId = redisTemplate.opsForValue().get(key);
        return storedUserId != null && storedUserId.equals(userId.toString());
    }

    public Long getRefreshToken(String token) {
        String key = REFRESH_PREFIX + token;
        String storedUserId = redisTemplate.opsForValue().get(key);
        return storedUserId != null ? Long.valueOf(storedUserId) : null;
    }


    public void deleteRefreshToken(String token, Long userId) {
        redisTemplate.delete(REFRESH_PREFIX + token);

        String userKey = REFRESH_SET_PREFIX + userId;
        redisTemplate.opsForSet().remove(userKey, token);
    }

    public Set<String> getAllRefreshTokensByUser(Long userId) {
        String userKey = REFRESH_SET_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userKey);
        return tokens != null ? tokens : Set.of();
    }

    // =========================
    // 🔹 OAuth2 임시 토큰 관리 (access, refresh, state)
    // =========================
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
