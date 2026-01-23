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
 * 클라이언트 IP 주소를 기반으로 Rate Limit 키를 생성합니다.
 * IP 추출 실패 시 fallback 키를 사용하여 Rate Limit 우회를 방지합니다.
 */
@Slf4j
@Component
public class IpRateLimitKeyStrategy implements RateLimitKeyStrategy {

    private static final String UNKNOWN_IP = "unknown";
    private static final String FALLBACK_IP = "fallback";

    @Override
    public Optional<RateLimitKey> buildKey(RateLimitRule rule, RateLimitFilterContext context) {
        String clientIp = HttpRequestUtils.getClientIpAddress(context.getRequest());
        
        // IP 추출 실패 시 fallback 키 사용 (Rate Limit 우회 방지)
        if (clientIp == null || clientIp.isEmpty() || UNKNOWN_IP.equalsIgnoreCase(clientIp)) {
            log.debug("Cannot extract client IP, using fallback key: rule={}", rule.getName());
            clientIp = FALLBACK_IP;
        }
        
        String keyValue = RedisKeyFactory.rateLimitByIp(rule.getName(), clientIp);
        return Optional.of(RateLimitKey.forIp(keyValue));
    }
}
