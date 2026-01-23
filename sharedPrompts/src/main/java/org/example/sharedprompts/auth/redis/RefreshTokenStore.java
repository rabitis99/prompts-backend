package org.example.sharedprompts.auth.redis;

import java.util.Set;

/**
 * Refresh Token 저장소 인터페이스
 * 
 * Refresh Token의 저장, 조회, 삭제를 담당합니다.
 * 1회용 보장을 위해 getAndDelete 메서드를 제공합니다.
 */
public interface RefreshTokenStore {
    
    /**
     * Refresh Token 저장 (기본 TTL 사용)
     * 
     * @param token Refresh Token
     * @param userId 사용자 ID
     * @param ip 클라이언트 IP 주소
     * @param userAgent User-Agent 헤더 값
     */
    void save(String token, Long userId, String ip, String userAgent);
    
    /**
     * Refresh Token 저장 (지정된 TTL 사용)
     * 
     * <p>회전하지 않는 경우 남은 TTL을 유지하기 위해 사용합니다.
     * 
     * @param token Refresh Token
     * @param userId 사용자 ID
     * @param ip 클라이언트 IP 주소
     * @param userAgent User-Agent 헤더 값
     * @param ttlMillis TTL (밀리초)
     */
    void saveWithTtl(String token, Long userId, String ip, String userAgent, long ttlMillis);
    
    /**
     * Refresh Token 조회 및 삭제 (1회용 보장)
     * 
     * @param token Refresh Token
     * @return RefreshTokenMetadata (토큰이 없거나 이미 사용된 경우 null)
     */
    RefreshTokenMetadata getAndDelete(String token);
    
    /**
     * Refresh Token 유효성 검증
     * 
     * @param token Refresh Token
     * @param userId 사용자 ID
     * @return 유효 여부
     */
    boolean isValid(String token, Long userId);
    
    /**
     * Refresh Token 삭제
     * 
     * @param token Refresh Token
     * @param userId 사용자 ID
     */
    void delete(String token, Long userId);
    
    /**
     * 사용자의 모든 Refresh Token 조회
     * 
     * @param userId 사용자 ID
     * @return Refresh Token Set
     */
    Set<String> getAllByUser(Long userId);
    
    /**
     * 사용자의 모든 Refresh Token 삭제
     * 
     * @param userId 사용자 ID
     */
    void deleteAllByUser(Long userId);
    
    /**
     * 사용자의 만료된 Refresh Token 정리 (Lazy Cleanup)
     * 
     * Set에 저장된 토큰 중 실제로 Redis에 존재하지 않는(만료된) 토큰을 제거합니다.
     * 
     * @param userId 사용자 ID
     */
    void cleanupExpiredTokens(Long userId);
}

