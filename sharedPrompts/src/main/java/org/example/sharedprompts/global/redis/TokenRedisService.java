package org.example.sharedprompts.global.redis;

import java.time.Duration;
import java.util.Map;

public interface TokenRedisService {

    // Access Token 관리
    void saveAccessToken(String token, Long userId);
    
    boolean isAccessTokenValid(String token);
    
    void deleteAccessToken(String token);

    // OAuth2 임시 토큰 관리
    void saveTempToken(String key, String accessToken, String refreshToken, String state,
                       String provider, String providerId, Duration ttl);
    
    Map<String, String> getAndDeleteTempToken(String key);
}
