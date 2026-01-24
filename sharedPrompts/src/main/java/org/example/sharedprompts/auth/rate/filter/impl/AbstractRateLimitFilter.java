package org.example.sharedprompts.auth.rate.filter.impl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitFilterContext;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitKey;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitResultWithKey;
import org.example.sharedprompts.auth.rate.filter.service.facade.RateLimitFacade;
import org.example.sharedprompts.auth.rate.filter.strategy.RateLimitKeyStrategy;
import org.example.sharedprompts.auth.rate.policy.RateLimitProperties;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Rate Limit 필터 추상 클래스 (Template Method 패턴)
 */
public abstract class AbstractRateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRateLimitFilter.class);

    protected final RateLimitFacade facade;
    protected final RateLimitKeyStrategy keyStrategy;
    protected final RateLimitProperties rateLimitProperties;

    protected AbstractRateLimitFilter(
            RateLimitFacade facade,
            RateLimitKeyStrategy keyStrategy,
            RateLimitProperties rateLimitProperties
    ) {
        this.facade = facade;
        this.keyStrategy = keyStrategy;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!shouldApplyFilter(request, response, filterChain)) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitFilterContext context = createContext(request);
        processRateLimitCheck(context, request, response, filterChain);
    }

    /**
     * Rate limit 체크 결과를 처리합니다.
     * 
     * @param result Rate limit 체크 결과
     * @param context Rate Limit 필터 컨텍스트
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     */
    private void handleRateLimitResult(
            RateLimitResultWithKey result,
            RateLimitFilterContext context,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (result.isExceeded()) {
            handleRateLimitExceeded(result, context, request, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Rate limit 초과 시 처리를 수행합니다.
     * 
     * @param result Rate limit 체크 결과
     * @param context Rate Limit 필터 컨텍스트
     * @param request HTTP 요청
     * @param response HTTP 응답
     */
    private void handleRateLimitExceeded(
            RateLimitResultWithKey result,
            RateLimitFilterContext context,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        // Rate limit 초과 로깅
        logRateLimitExceeded(
                result.getRule(),
                result.getKey(),
                result.getResult(),
                context,
                request
        );

        // Rate limit 초과 응답 처리
        // logCallback은 빈 람다를 전달하여 중복 로깅을 방지합니다.
        // logCallback은 handle() 메서드 내부에서 recordLog()를 통해 호출되며,
        // 필요시 추가적인 로깅이나 후처리를 수행할 수 있는 확장 포인트 역할을 합니다.
        facade.getExceededFacade().handle(
                result.getRule(),
                result.getKey(),
                result.getResult(),
                response,
                (k, r) -> {}
        );
    }

    /**
     * Rate limit 체크를 수행하고 결과에 따라 요청을 처리합니다.
     * 
     * @param context Rate Limit 필터 컨텍스트
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     */
    private void processRateLimitCheck(
            RateLimitFilterContext context,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Optional<RateLimitResultWithKey> result;
        try {
            result = performRateLimitCheck(context, request);
        } catch (Exception e) {
            handleRateLimitCheckFailure(e, filterChain, request, response);
            return;
        }

        if (result.isPresent()) {
            handleRateLimitResult(result.get(), context, request, response, filterChain);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Rate limit 체크를 수행합니다.
     * 
     * @param context Rate Limit 필터 컨텍스트
     * @param request HTTP 요청
     * @return Rate limit 체크 결과 (규칙이 없거나 키를 생성할 수 없는 경우 Optional.empty())
     */
    private Optional<RateLimitResultWithKey> performRateLimitCheck(
            RateLimitFilterContext context,
            HttpServletRequest request
    ) {
        Optional<RateLimitRule> ruleOpt = resolveRule(request);
        if (ruleOpt.isEmpty()) {
            return Optional.empty();
        }

        RateLimitRule rule = ruleOpt.get();
        Optional<RateLimitKey> keyOpt = buildRateLimitKey(rule, context);
        if (keyOpt.isEmpty()) {
            return Optional.empty();
        }

        RateLimitKey key = keyOpt.get();
        return processRateLimitRule(rule, key, context);
    }

    /**
     * 요청에 대한 Rate limit 규칙을 해석합니다.
     * 
     * @param request HTTP 요청
     * @return Rate limit 규칙 (규칙이 없는 경우 Optional.empty())
     */
    private Optional<RateLimitRule> resolveRule(HttpServletRequest request) {
        return facade.getRuleResolutionService().resolveRule(request);
    }

    /**
     * Rate limit 키를 생성합니다.
     * 
     * @param rule Rate limit 규칙
     * @param context Rate Limit 필터 컨텍스트
     * @return Rate limit 키 (키를 생성할 수 없는 경우 Optional.empty())
     */
    private Optional<RateLimitKey> buildRateLimitKey(RateLimitRule rule, RateLimitFilterContext context) {
        return keyStrategy.buildKey(rule, context);
    }

    /**
     * Rate limit 규칙을 처리하고 결과를 반환합니다.
     * 
     * @param rule Rate limit 규칙
     * @param key Rate limit 키
     * @param context Rate Limit 필터 컨텍스트
     * @return Rate limit 체크 결과
     */
    private Optional<RateLimitResultWithKey> processRateLimitRule(
            RateLimitRule rule,
            RateLimitKey key,
            RateLimitFilterContext context
    ) {
        return facade.getProcessor()
                .processRule(rule, context, r -> Optional.of(key));
    }

    /**
     * Rate limit 체크 실패 시 정책에 따라 요청을 처리합니다.
     * 
     * @param e 발생한 예외
     * @param filterChain 필터 체인
     * @param request HTTP 요청
     * @param response HTTP 응답
     */
    private void handleRateLimitCheckFailure(
            Exception e,
            FilterChain filterChain,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        boolean failOpen = rateLimitProperties.getFailurePolicy().isFailOpen();
        if (failOpen) {
            logger.warn("Rate limit check failed, allowing request (Fail-Open)", e);
            filterChain.doFilter(request, response);
        } else {
            logger.error("Rate limit check failed, blocking request (Fail-Closed)", e);
            sendServiceUnavailableResponse(response);
        }
    }

    /**
     * 서비스 불가 응답을 전송합니다.
     * 
     * @param response HTTP 응답
     */
    private void sendServiceUnavailableResponse(HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service temporarily unavailable");
        } catch (IOException ioException) {
            logger.error("Failed to send error response", ioException);
            throw new RuntimeException(ioException);
        }
    }

    /**
     * 필터 적용 여부 결정
     */
    protected boolean shouldApplyFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        return true;
    }

    protected abstract RateLimitFilterContext createContext(HttpServletRequest request);

    protected abstract void logRateLimitExceeded(
            RateLimitRule rule,
            RateLimitKey key,
            RateLimiter.RateLimitResult result,
            RateLimitFilterContext context,
            HttpServletRequest request
    );

}
