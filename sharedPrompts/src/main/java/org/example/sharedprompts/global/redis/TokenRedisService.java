package org.example.sharedprompts.global.redis;

import java.time.Duration;
import java.util.Map;

public interface TokenRedisService {

    // Access Token 관리
    void saveAccessToken(String token, Long userId);
    
    boolean isAccessTokenValid(String token);
    
    void deleteAccessToken(String token);

    // OAuth2 임시 인증 세션 관리
    void saveOAuth2TempSession(String tempKey, String provider, String providerId, String state, Duration ttl);
    
    Map<String, String> getAndDeleteOAuth2TempSession(String tempKey);
}
