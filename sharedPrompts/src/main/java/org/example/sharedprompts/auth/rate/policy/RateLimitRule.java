package org.example.sharedprompts.auth.rate.policy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static org.example.sharedprompts.auth.rate.policy.RateLimitConstants.RuleNames;

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
        this(name, capacity, 60L); // 기본값은 Properties에서 관리
    }
    
    /**
     * 사전 정의된 Rate Limit 규칙들
     * 
     * @deprecated 이 클래스는 더 이상 사용되지 않습니다.
     *             RateLimitRuleConfig를 통해 Properties 기반 규칙을 사용하세요.
     */
    @Deprecated
    public static class Predefined {
        /**
         * @deprecated RateLimitRuleConfig.createMatchers()를 사용하세요.
         */
        @Deprecated
        public static final RateLimitRule LOGIN = new RateLimitRule(RuleNames.LOGIN, 5L);
        
        /**
         * @deprecated RateLimitRuleConfig.createMatchers()를 사용하세요.
         */
        @Deprecated
        public static final RateLimitRule SIGNUP = new RateLimitRule(RuleNames.SIGNUP, 3L);
        
        /**
         * @deprecated RateLimitRuleConfig.createMatchers()를 사용하세요.
         */
        @Deprecated
        public static final RateLimitRule PROMPT_CREATE = new RateLimitRule(RuleNames.PROMPT_CREATE, 10L);
        
        /**
         * @deprecated RateLimitRuleConfig.createMatchers()를 사용하세요.
         */
        @Deprecated
        public static final RateLimitRule GENERAL = new RateLimitRule(RuleNames.GENERAL, 100L);
    }
}

