package org.example.sharedprompts.auth.security;

/**
 * 토큰 보안 검증 결과
 */
public enum TokenSecurityCheckResult {
    /**
     * IP와 User-Agent 모두 일치
     */
    MATCH,
    
    /**
     * 부분 일치 또는 유사 (같은 서브넷, 같은 브라우저 등)
     */
    SUSPICIOUS,
    
    /**
     * 완전 불일치 (보안 위협 가능성)
     */
    MISMATCH
}

