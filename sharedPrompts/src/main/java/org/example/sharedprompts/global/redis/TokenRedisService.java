package org.example.sharedprompts.global.redis;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.config.TokenTtlConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenRedisService {

    private final StringRedisTemplate redisTemplate;
    private final TokenTtlConfig ttlConfig ;

    private static final String ACCESS_PREFIX = "ACCESS:";
    private static final String REFRESH_SET_PREFIX = "USER_REFRESH:";

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
}
