package org.example.sharedprompts.auth.rate.policy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static org.example.sharedprompts.auth.rate.policy.RateLimitConstants.Capacities;
import static org.example.sharedprompts.auth.rate.policy.RateLimitConstants.RuleNames;
import static org.example.sharedprompts.auth.rate.policy.RateLimitConstants.Windows;

/**
 * Rate Limit 규칙 정의
 * 
 * 각 API 엔드포인트별 Rate Limit 정책을 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public class RateLimitRule {
    
    private final String name;
    private final long capacity;
    private final long windowSeconds;
    
    /**
     * 기본 윈도우(60초)를 사용하는 규칙 생성
     */
    public RateLimitRule(String name, long capacity) {
        this(name, capacity, Windows.DEFAULT_SECONDS);
    }
    
    /**
     * 사전 정의된 Rate Limit 규칙들
     */
    public static class Predefined {
        public static final RateLimitRule LOGIN = new RateLimitRule(RuleNames.LOGIN, Capacities.LOGIN);
        public static final RateLimitRule SIGNUP = new RateLimitRule(RuleNames.SIGNUP, Capacities.SIGNUP);
        public static final RateLimitRule PROMPT_CREATE = new RateLimitRule(RuleNames.PROMPT_CREATE, Capacities.PROMPT_CREATE);
        public static final RateLimitRule GENERAL = new RateLimitRule(RuleNames.GENERAL, Capacities.GENERAL);
    }
}

