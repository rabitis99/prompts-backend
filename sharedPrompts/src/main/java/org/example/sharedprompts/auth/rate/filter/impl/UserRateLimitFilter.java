package org.example.sharedprompts.auth.rate.filter.impl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitFilterContext;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitKey;
import org.example.sharedprompts.auth.rate.filter.metrics.RateLimitMetricsCollector;
import org.example.sharedprompts.auth.rate.filter.service.facade.RateLimitFacade;
import org.example.sharedprompts.auth.rate.filter.strategy.UserRateLimitKeyStrategy;
import org.example.sharedprompts.auth.rate.filter.util.auth.AuthenticationHelper;
import org.example.sharedprompts.auth.rate.policy.RateLimitProperties;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 사용자 기반 Rate Limiting 필터
 *
 * 인증된 사용자의 ID를 기준으로 Rate Limiting을 수행합니다.
 * 인증되지 않은 사용자는 이 필터를 통과합니다 (IP 기반 필터에서 처리).
 *
 * 특징:
 * - 동일한 사용자가 여러 IP에서 접근하더라도 사용자 ID 기준으로 제한됩니다.
 * - 인증되지 않은 요청은 자동으로 필터를 통과합니다.
 */
@Component
@Order(-50) // IP 기반 RateLimit 이후, 인증 필터 이후 실행
public class UserRateLimitFilter extends AbstractRateLimitFilter {

    public UserRateLimitFilter(
            RateLimitFacade facade,
            UserRateLimitKeyStrategy keyStrategy,
            RateLimitProperties rateLimitProperties,
            RateLimitMetricsCollector metricsCollector,
            RedisTemplate<String, Object> redisTemplate
    ) {
        super(facade, keyStrategy, rateLimitProperties, metricsCollector, redisTemplate);
    }

    @Override
    protected boolean shouldApplyFilter(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) {
        // 인증되지 않은 요청은 IP 기반 RateLimit에서 처리
        return AuthenticationHelper.getAuthentication().isPresent();
    }

    @Override
    protected RateLimitFilterContext createContext(HttpServletRequest request) {
        Optional<Authentication> authOpt = AuthenticationHelper.getAuthentication();
        Optional<Long> userIdOpt = authOpt.flatMap(AuthenticationHelper::extractUserId);

        // shouldApplyFilter에서 인증 존재를 확인하므로
        // 실제 처리 경로에서는 userId가 항상 존재하는 것이 정상 케이스
        return RateLimitFilterContext.forUser(
                request,
                authOpt.orElse(null),
                userIdOpt.orElse(null)
        );
    }

    @Override
    protected void logRateLimitExceeded(
            RateLimitRule rule,
            RateLimitKey key,
            RateLimiter.RateLimitResult result,
            RateLimitFilterContext context,
            HttpServletRequest request
    ) {
        Long userId = context.getUserIdOpt().orElse(null);
        facade.getLoggingService()
                .logRateLimitExceeded(rule, key, result, request, userId);
    }
}
