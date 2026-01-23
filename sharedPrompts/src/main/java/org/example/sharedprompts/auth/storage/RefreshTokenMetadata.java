package org.example.sharedprompts.auth.storage;

import lombok.Getter;

/**
 * Refresh Token 메타데이터
 * 
 * Refresh Token과 함께 저장되는 보안 관련 정보
 */
@Getter
public class RefreshTokenMetadata {
    
    private final Long userId;
    private final String ip;
    private final String userAgent;
    private final long remainingTtlMillis;
    
    public RefreshTokenMetadata(Long userId, String ip, String userAgent, long remainingTtlMillis) {
        this.userId = userId;
        this.ip = ip != null ? ip : "";
        this.userAgent = userAgent != null ? userAgent : "";
        this.remainingTtlMillis = remainingTtlMillis;
    }
}


