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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

/**
 * Rate Limit 필터 추상 클래스
 * 
 * IP 기반 및 사용자 기반 Rate Limit 필터의 공통 로직을 제공합니다.
 * Template Method 패턴을 사용하여 공통 흐름을 정의하고,
 * 하위 클래스에서 특화된 로직을 구현하도록 합니다.
 */
@RequiredArgsConstructor
public abstract class AbstractRateLimitFilter extends OncePerRequestFilter {

    protected final RateLimitFacade facade;
    protected final RateLimitKeyStrategy keyStrategy;

    /**
     * 응답 처리 완료 여부를 나타내는 플래그 (스레드별로 독립적)
     * 
     * filterChain.doFilter() 호출 또는 rate limit 초과 응답 작성 등
     * 응답이 처리되었는지 여부를 추적합니다.
     * 예외 발생 시 catch 블록에서 이중 처리 방지를 위해 사용됩니다.
     * 
     * ThreadLocal을 사용하여 각 요청 스레드별로 독립적인 값을 유지합니다.
     * OncePerRequestFilter는 싱글톤이므로 인스턴스 필드는 모든 요청 간에 공유됩니다.
     */
    private static final ThreadLocal<Boolean> responseHandled = ThreadLocal.withInitial(() -> false);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // 플래그 초기화 (스레드별로 독립적)
        responseHandled.set(false);

        try {
            // 필터 적용 전 검증 (하위 클래스에서 구현)
            if (!shouldApplyFilter(request, response, filterChain)) {
                responseHandled.set(true);
                filterChain.doFilter(request, response);
                return;
            }

            // 컨텍스트 생성 (하위 클래스에서 구현)
            RateLimitFilterContext context = createContext(request);

            // 요청 검증 및 규칙 결정
            Optional<RateLimitRule> ruleOpt = facade.getRuleResolutionService().resolveRule(request);
            if (ruleOpt.isEmpty()) {
                responseHandled.set(true);
                filterChain.doFilter(request, response);
                return;
            }

            // Rate Limit 처리
            RateLimitRule rule = ruleOpt.get();
            Function<RateLimitRule, Optional<RateLimitKey>> buildKeyFunction = r -> keyStrategy.buildKey(r, context);

            // Processor에서 Rate Limit 체크 수행 (순수 도메인 로직)
            Optional<RateLimitResultWithKey> resultOpt = facade.getProcessor().processRule(
                    rule,
                    context,
                    buildKeyFunction
            );

            // 키 생성 실패 시 요청 허용
            if (resultOpt.isEmpty()) {
                responseHandled.set(true);
                filterChain.doFilter(request, response);
                return;
            }

            // 결과에 따라 HTTP 응답 처리
            RateLimitResultWithKey result = resultOpt.get();
            handleRateLimitResult(result, context, request, response, filterChain);

        } catch (Exception e) {
            // 예외 발생 시 요청 허용 (Fail Open 정책)
            // 이미 응답이 처리된 경우 예외를 재발생시켜 이중 처리 방지
            if (responseHandled.get()) {
                if (e instanceof ServletException) {
                    throw (ServletException) e;
                } else if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    throw new ServletException(e);
                }
            }
            // 응답이 처리되지 않은 경우에만 filterChain 호출
            // 플래그를 먼저 설정하여 중복 호출 방지
            responseHandled.set(true);
            filterChain.doFilter(request, response);
        } finally {
            // ThreadLocal 메모리 누수 방지를 위해 정리
            responseHandled.remove();
        }
    }

    /**
     * Rate Limit 결과를 처리합니다.
     * 
     * @param result RateLimitResultWithKey
     * @param context RateLimitFilterContext
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param filterChain FilterChain
     * @throws ServletException
     * @throws IOException
     */
    private void handleRateLimitResult(
            RateLimitResultWithKey result,
            RateLimitFilterContext context,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (result.isExceeded()) {
            // Rate Limit 초과 처리
            RateLimitRule rule = result.getRule();
            RateLimitKey key = result.getKey();
            RateLimiter.RateLimitResult rateLimitResult = result.getResult();

            // 로그 기록 (콜백)
            logRateLimitExceeded(rule, key, rateLimitResult, context, request);

            // HTTP 응답 작성
            // 플래그를 먼저 설정하여 예외 발생 시 중복 처리 방지
            responseHandled.set(true);
            facade.getExceededFacade().handle(
                    rule,
                    key,
                    rateLimitResult,
                    response,
                    (k, r) -> {} // 로그는 이미 기록됨
            );
        } else {
            // Rate Limit 통과
            // 플래그를 먼저 설정하여 예외 발생 시 중복 호출 방지
            responseHandled.set(true);
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 필터 적용 여부를 결정합니다.
     * 
     * 기본 구현: 항상 적용
     * 하위 클래스에서 오버라이드하여 특정 조건에서만 적용하도록 할 수 있습니다.
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param filterChain FilterChain
     * @return 필터 적용 여부
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
     * 
     * @param request HttpServletRequest
     * @return RateLimitFilterContext
     */
    protected abstract RateLimitFilterContext createContext(HttpServletRequest request);

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
