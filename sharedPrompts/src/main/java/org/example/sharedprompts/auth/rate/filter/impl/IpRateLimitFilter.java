package org.example.sharedprompts.auth.rate.filter.impl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitFilterContext;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitKey;
import org.example.sharedprompts.auth.rate.filter.service.facade.RateLimitFacade;
import org.example.sharedprompts.auth.rate.filter.strategy.IpRateLimitKeyStrategy;
import org.example.sharedprompts.auth.rate.policy.RateLimitProperties;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * IP 기반 Rate Limiting 필터
 *
 * 클라이언트 IP 주소를 기준으로 Rate Limiting을 수행합니다.
 * 인증되지 않은 사용자도 IP 기반으로 제한할 수 있습니다.
 *
 * Rate Limit 정책 (docs/backend/04_SECURITY.md 기준):
 * - 로그인: 5회/분
 * - 회원가입: 3회/분
 * - 프롬프트 생성: 10회/분
 * - 일반 API: 100회/분
 */
@Component
@Order(-200)
public class IpRateLimitFilter extends AbstractRateLimitFilter {

    public IpRateLimitFilter(
            RateLimitFacade facade,
            IpRateLimitKeyStrategy keyStrategy,
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
        // IP 기반 필터는 인증 여부와 관계없이 모든 요청에 적용
        return true;
    }

    @Override
    protected RateLimitFilterContext createContext(HttpServletRequest request) {
        return RateLimitFilterContext.forIp(request);
    }

    @Override
    protected void logRateLimitExceeded(
            RateLimitRule rule,
            RateLimitKey key,
            RateLimiter.RateLimitResult result,
            RateLimitFilterContext context,
            HttpServletRequest request
    ) {
        facade.getLoggingService().logRateLimitExceeded(rule, key, result, request, null);
    }
}
