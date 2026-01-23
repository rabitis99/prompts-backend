package org.example.sharedprompts.auth.rate.filter.impl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitFilterContext;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitKey;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitResultWithKey;
import org.example.sharedprompts.auth.rate.filter.service.facade.RateLimitFacade;
import org.example.sharedprompts.auth.rate.filter.strategy.RateLimitKeyStrategy;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Rate Limit 필터 추상 클래스
 * 
 * IP 기반과 사용자 기반 Rate Limit 필터의 공통 로직을 제공합니다.
 * Facade 패턴을 통해 의존성을 단순화했습니다.
 */
@RequiredArgsConstructor
public abstract class AbstractRateLimitFilter extends OncePerRequestFilter {

    protected final RateLimitFacade facade;
    protected final RateLimitKeyStrategy keyStrategy;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // 필터 적용 전 검증 (하위 클래스에서 구현)
            if (!shouldApplyFilter(request, response, filterChain)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 컨텍스트 생성 (하위 클래스에서 구현)
            RateLimitFilterContext context = createContext(request);

            // 요청 검증 및 규칙 결정
            Optional<RateLimitRule> ruleOpt = facade.getRuleResolutionService().resolveRule(request);
            if (ruleOpt.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            // Rate Limit 처리
            RateLimitRule rule = ruleOpt.get();
            Function<RateLimitRule, Optional<RateLimitKey>> buildKeyFunction = 
                    r -> keyStrategy.buildKey(r, context);

            // Processor에서 Rate Limit 체크 수행 (순수 도메인 로직)
            Optional<RateLimitResultWithKey> resultOpt = facade.getProcessor().processRule(
                    rule, context, buildKeyFunction
            );

            // 키 생성 실패 시 요청 허용
            if (resultOpt.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            // 결과에 따라 HTTP 응답 처리
            RateLimitResultWithKey result = resultOpt.get();
            handleRateLimitResult(result, context, request, response, filterChain);
            
        } catch (Exception e) {
            // 예외 발생 시 요청 허용 (Fail Open 정책)
            // 로깅은 전역 예외 핸들러에서 처리하거나, 필요시 여기서도 로깅 가능
            // 중요한 예외는 로깅하여 모니터링 가능하도록 함
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 필터 적용 여부를 결정합니다.
     * 
     * 하위 클래스에서 필요시 오버라이드하여 인증 상태 등을 확인할 수 있습니다.
     * false를 반환하면 필터 체인을 계속 진행합니다 (요청 허용).
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param filterChain FilterChain
     * @return 필터를 적용할지 여부 (true: 필터 적용, false: 필터 체인 계속 진행)
     */
    protected boolean shouldApplyFilter(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        return true;
    }

    /**
     * Rate Limit 필터 컨텍스트를 생성합니다.
     * 하위 클래스에서 IP 또는 사용자 기반으로 구현합니다.
     * 
     * @param request HttpServletRequest
     * @return RateLimitFilterContext
     */
    protected abstract RateLimitFilterContext createContext(HttpServletRequest request);

    /**
     * Rate Limit 결과에 따라 HTTP 응답을 처리합니다.
     * 
     * @param result RateLimitResultWithKey
     * @param context RateLimitFilterContext
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param filterChain FilterChain
     */
    private void handleRateLimitResult(
            RateLimitResultWithKey result,
            RateLimitFilterContext context,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {
        // Fail Open: 체크 실패 시 요청 허용
        if (result.isFailed()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Rate Limit 초과 처리
        if (result.isExceeded()) {
            ErrorCode errorCode = getErrorCode(
                    result.getRule(), 
                    result.getKey(), 
                    context, 
                    request
            );
            
            // HTTP 응답 처리 (로그는 logCallback에서 처리)
            BiConsumer<RateLimitKey, RateLimiter.RateLimitResult> logCallback = 
                    (k, r) -> logRateLimitExceeded(result.getRule(), k, r, context, request);
            
            facade.getExceededFacade().handle(
                    result.getRule(), 
                    result.getKey(), 
                    result.getResult(), 
                    response,
                    logCallback,
                    errorCode
            );
            return;
        }

        // 정상 요청 허용
        filterChain.doFilter(request, response);
    }

    /**
     * Rate Limit 초과 시 사용할 에러 코드를 결정합니다.
     * 
     * 하위 클래스에서 오버라이드하여 규칙별, 타입별로 다른 에러 코드를 사용할 수 있습니다.
     * 예를 들어, 로그인 API는 다른 에러 코드를 사용하거나, IP 기반과 사용자 기반에 따라
     * 다른 에러 코드를 반환할 수 있습니다.
     * 
     * 기본 구현은 ErrorCode.RATE_LIMIT_EXCEEDED를 반환합니다.
     * 
     * @param rule RateLimitRule
     * @param key Rate Limit 키 (타입 정보 포함)
     * @param context RateLimitFilterContext
     * @param request HttpServletRequest
     * @return 사용할 ErrorCode (기본값: RATE_LIMIT_EXCEEDED)
     */
    protected ErrorCode getErrorCode(
            RateLimitRule rule,
            RateLimitKey key,
            RateLimitFilterContext context,
            HttpServletRequest request
    ) {
        return ErrorCode.RATE_LIMIT_EXCEEDED;
    }

    /**
     * Rate Limit 초과 로그를 기록합니다.
     * 하위 클래스에서 IP 또는 사용자 기반으로 구현합니다.
     * 
     * @param rule RateLimitRule
     * @param key Rate Limit 키 (타입 정보 포함)
     * @param result RateLimitResult
     * @param context RateLimitFilterContext
     * @param request HttpServletRequest
     */
    protected abstract void logRateLimitExceeded(
            RateLimitRule rule,
            RateLimitKey key,
            RateLimiter.RateLimitResult result,
            RateLimitFilterContext context,
            HttpServletRequest request
    );
}
