package org.example.sharedprompts.domain.rate.ratelimitlog.enums;

/**
 * Rate Limit 타입 열거형
 * 
 * Rate Limit이 적용되는 기준을 나타냅니다.
 */
public enum RateLimitType {
    /**
     * IP 기반 Rate Limit
     * 클라이언트 IP 주소를 기준으로 제한합니다.
     */
    IP,
    
    /**
     * 사용자 기반 Rate Limit
     * 인증된 사용자 ID를 기준으로 제한합니다.
     */
    USER
}






