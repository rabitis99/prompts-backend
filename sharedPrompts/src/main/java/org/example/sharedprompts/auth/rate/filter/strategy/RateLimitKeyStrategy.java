package org.example.sharedprompts.auth.rate.filter.strategy;

import org.example.sharedprompts.auth.rate.filter.model.RateLimitFilterContext;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitKey;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;

import java.util.Optional;

/**
 * Rate Limit 키 생성 전략 인터페이스
 * 
 * 다양한 키 생성 전략을 구현할 수 있도록 인터페이스로 정의합니다.
 */
public interface RateLimitKeyStrategy {

    /**
     * Rate Limit 키를 생성합니다.
     * 
     * @param rule RateLimitRule
     * @param context RateLimitFilterContext
     * @return Optional<RateLimitKey> Rate Limit 키 (생성 불가능한 경우 empty)
     */
    Optional<RateLimitKey> buildKey(RateLimitRule rule, RateLimitFilterContext context);
}


