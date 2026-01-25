package org.example.sharedprompts.auth.storage;

/**
 * Redis 키 네임스페이스 팩토리
 * 
 * 인증 관련 Redis 키를 표준화된 형식으로 생성합니다.
 */
public class RedisKeyFactory {
    
    private static final String AUTH_PREFIX = "auth";
    private static final String RATE_PREFIX = "rate";
    private static final String OAUTH_PREFIX = "oauth";
    private static final String OAUTH_TEMP_PREFIX = "oauth:temp:";
    /**
     * Refresh Token Set 키의 prefix
     * Lua 스크립트에서 동적으로 키를 구성할 때 사용됩니다.
     */
    public static final String REFRESH_TOKEN_SET_PREFIX = AUTH_PREFIX + ":refresh:set:";
    
    /**
     * Refresh Token Set 키의 prefix를 반환합니다.
     * 
     * @return Refresh Token Set 키의 prefix
     */
    public static String getRefreshTokenSetPrefix() {
        return REFRESH_TOKEN_SET_PREFIX;
    }
    
    /**
     * Access Token 키 생성
     */
    public static String accessToken(String token) {
        return String.format("%s:access:%s", AUTH_PREFIX, token);
    }
    
    /**
     * Refresh Token 키 생성
     */
    public static String refreshToken(String token) {
        return String.format("%s:refresh:%s", AUTH_PREFIX, token);
    }
    
    /**
     * Refresh Token 키의 prefix를 반환합니다.
     * Lua 스크립트에서 토큰 키를 동적으로 구성할 때 사용됩니다.
     * 
     * @return Refresh Token 키의 prefix (예: "auth:refresh:")
     */
    public static String getRefreshTokenPrefix() {
        return AUTH_PREFIX + ":refresh:";
    }
    
    /**
     * 사용자별 Refresh Token Set 키 생성
     */
    public static String refreshTokenSet(Long userId) {
        return String.format("%s:refresh:set:%d", AUTH_PREFIX, userId);
    }
    
    /**
     * Token Version 키 생성
     */
    public static String tokenVersion(Long userId) {
        return String.format("%s:version:%d", AUTH_PREFIX, userId);
    }
    
    /**
     * IP 기반 Rate Limit 키 생성
     */
    public static String rateLimitByIp(String rule, String ip) {
        return String.format("%s:%s:ip:%s", RATE_PREFIX, rule, ip);
    }
    
    /**
     * 사용자 기반 Rate Limit 키 생성
     */
    public static String rateLimitByUser(String rule, Long userId) {
        return String.format("%s:%s:user:%d", RATE_PREFIX, rule, userId);
    }
    
    /**
     * OAuth2 State 키 생성
     */
    public static String oauthState(String value) {
        return String.format("%s:state:%s", OAUTH_PREFIX, value);
    }
    
    /**
     * OAuth2 임시 토큰 키 생성
     */
    public static String oauthTemp(String key) {
        return OAUTH_TEMP_PREFIX + key;
    }

}


