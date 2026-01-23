package org.example.sharedprompts.global.redis;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

public interface TokenRedisService {

    // Access Token 관리
    void saveAccessToken(String token, Long userId);
    
    boolean isAccessTokenValid(String token);
    
    void deleteAccessToken(String token);
    
    boolean isAccessTokenValidWithUserId(String accessToken, Long userId);

    // Refresh Token 관리
    /**
     * @deprecated 이 메서드는 더 이상 사용되지 않습니다.
     * {@link org.example.sharedprompts.auth.redis.RefreshTokenStore#save(String, Long, String, String)}를 사용하세요.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    void saveRefreshToken(String token, Long userId);
    
    boolean isRefreshTokenValid(String token, Long userId);
    
    Long getRefreshToken(String token);
    
    void deleteRefreshToken(String token, Long userId);
    
    Set<String> getAllRefreshTokensByUser(Long userId);

    // OAuth2 임시 토큰 관리
    void saveTempToken(String key, String accessToken, String refreshToken, String state,
                       String provider, String providerId, Duration ttl);
    
    Map<String, String> getAndDeleteTempToken(String key);
}
