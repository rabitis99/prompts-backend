package org.example.sharedprompts.auth.rate.policy;

import org.springframework.http.HttpMethod;

/**
 * Rate Limit 규칙 매칭 인터페이스
 * 
 * URI와 HTTP Method를 기반으로 Rate Limit 규칙을 매칭합니다.
 */
public interface RateLimitMatcher {
    
    /**
     * URI와 HTTP Method가 이 매처의 조건에 맞는지 확인합니다.
     * 
     * @param uri 요청 URI
     * @param method HTTP Method
     * @return 매칭 여부
     */
    boolean matches(String uri, HttpMethod method);
    
    /**
     * 매칭된 경우 반환할 Rate Limit 규칙
     * 
     * @return RateLimitRule
     */
    RateLimitRule getRule();
}

