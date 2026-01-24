package org.example.sharedprompts.auth.rate.filter.impl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitFilterContext;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitKey;
import org.example.sharedprompts.auth.rate.filter.service.facade.RateLimitFacade;
import org.example.sharedprompts.auth.rate.filter.strategy.UserRateLimitKeyStrategy;
import org.example.sharedprompts.auth.rate.filter.util.auth.AuthenticationHelper;
import org.example.sharedprompts.auth.rate.policy.RateLimitProperties;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
@Order(-50)
public class UserRateLimitFilter extends AbstractRateLimitFilter {

    public UserRateLimitFilter(
            RateLimitFacade facade,
            UserRateLimitKeyStrategy keyStrategy,
            RateLimitProperties rateLimitProperties
    ) {
        super(facade, keyStrategy, rateLimitProperties);
    }

    @Override
    protected boolean shouldApplyFilter(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // 인증되지 않은 사용자는 통과 (IP 기반 필터에서 처리)
        Optional<Long> userIdOpt = AuthenticationHelper.getCurrentUserId();
        return userIdOpt.isPresent();
    }

    @Override
    protected RateLimitFilterContext createContext(HttpServletRequest request) {
        Optional<Authentication> authOpt = AuthenticationHelper.getAuthentication();
        Optional<Long> userIdOpt = authOpt.flatMap(AuthenticationHelper::extractUserId);
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
        facade.getLoggingService().logRateLimitExceeded(rule, key, result, request, userId);
    }
}
