package org.example.sharedprompts.auth.rate.filter.strategy;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitFilterContext;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitKey;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.example.sharedprompts.auth.redis.RedisKeyFactory;
import org.example.sharedprompts.global.util.HttpRequestUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * IP 기반 Rate Limit 키 생성 전략
 * 
 * IP 추출 실패 시(unknown) empty를 반환하여 Rate Limit 체크를 스킵합니다.
 */
@Slf4j
@Component
public class IpRateLimitKeyStrategy implements RateLimitKeyStrategy {

    private static final String UNKNOWN_IP = "unknown";

    @Override
    public Optional<RateLimitKey> buildKey(RateLimitRule rule, RateLimitFilterContext context) {
        String clientIp = HttpRequestUtils.getClientIpAddress(context.getRequest());
        
        // IP 추출 실패 시 empty 반환 (Rate Limit 체크 스킵)
        if (clientIp == null || clientIp.isEmpty() || UNKNOWN_IP.equalsIgnoreCase(clientIp)) {
            log.debug("Cannot extract client IP, skipping rate limit check: rule={}", rule.getName());
            return Optional.empty();
        }
        
        String keyValue = RedisKeyFactory.rateLimitByIp(rule.getName(), clientIp);
        return Optional.of(RateLimitKey.forIp(keyValue));
    }
}


