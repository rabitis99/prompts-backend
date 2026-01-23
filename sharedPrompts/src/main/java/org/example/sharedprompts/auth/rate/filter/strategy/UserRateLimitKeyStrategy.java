package org.example.sharedprompts.auth.rate.filter.strategy;

import org.example.sharedprompts.auth.rate.filter.model.RateLimitFilterContext;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitKey;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.example.sharedprompts.auth.redis.RedisKeyFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 사용자 기반 Rate Limit 키 생성 전략
 */
@Component
public class UserRateLimitKeyStrategy implements RateLimitKeyStrategy {

    @Override
    public Optional<RateLimitKey> buildKey(RateLimitRule rule, RateLimitFilterContext context) {
        return context.getUserIdOpt()
                .map(userId -> {
                    String keyValue = RedisKeyFactory.rateLimitByUser(rule.getName(), userId);
                    return RateLimitKey.forUser(keyValue);
                });
    }
}


