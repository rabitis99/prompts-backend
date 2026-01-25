package org.example.sharedprompts.auth.rate.filter.impl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitFilterContext;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitKey;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitResultWithKey;
import org.example.sharedprompts.auth.rate.filter.metrics.RateLimitMetricsCollector;
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
    protected final RateLimitMetricsCollector metricsCollector;

    protected AbstractRateLimitFilter(
            RateLimitFacade facade,
            RateLimitKeyStrategy keyStrategy,
            RateLimitProperties rateLimitProperties,
            RateLimitMetricsCollector metricsCollector
    ) {
        this.facade = facade;
        this.keyStrategy = keyStrategy;
        this.rateLimitProperties = rateLimitProperties;
        this.metricsCollector = metricsCollector;
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
     * Rate limit 초과 시 처리
     */
    private void handleRateLimitExceeded(
            RateLimitResultWithKey result,
            RateLimitFilterContext context,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        logRateLimitExceeded(
                result.getRule(),
                result.getKey(),
                result.getResult(),
                context,
                request
        );

        // Exceeded 응답 처리 (중복 로깅 방지를 위해 빈 콜백 전달)
        facade.getExceededFacade().handle(
                result.getRule(),
                result.getKey(),
                result.getResult(),
                response,
                (k, r) -> {}
        );
    }

    /**
     * Rate limit 체크 메인 흐름
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
            // Redis 등 인프라 장애 상황만 정책적으로 처리됨을 전제로 함
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
     * Rate limit 체크 수행
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
     * 요청에 대한 Rate limit 규칙 해석
     */
    private Optional<RateLimitRule> resolveRule(HttpServletRequest request) {
        return facade.getRuleResolutionService().resolveRule(request);
    }

    /**
     * Rate limit 키 생성
     */
    private Optional<RateLimitKey> buildRateLimitKey(
            RateLimitRule rule,
            RateLimitFilterContext context
    ) {
        return keyStrategy.buildKey(rule, context);
    }

    /**
     * Rate limit 규칙 처리
     */
    private Optional<RateLimitResultWithKey> processRateLimitRule(
            RateLimitRule rule,
            RateLimitKey key,
            RateLimitFilterContext context
    ) {
        return facade.getProcessor()
                .processRule(rule, context, ignored -> Optional.of(key));
    }

    /**
     * Rate limit 체크 실패 시 처리
     *
     * Redis 장애 등 인프라 오류 발생 시,
     * Failure Policy(Fail-Open / Fail-Closed)에 따라 요청을 처리합니다.
     */
    private void handleRateLimitCheckFailure(
            Exception e,
            FilterChain filterChain,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        boolean failOpen = rateLimitProperties.getFailurePolicy().isFailOpen();
        boolean logFailure = rateLimitProperties.getFailurePolicy().isLogFailure();

        Optional<RateLimitRule> ruleOpt = Optional.empty();
        try {
            ruleOpt = resolveRule(request);
        } catch (Exception ignored) {
            // 이미 실패 상황이므로 추가 오류는 무시
        }

        if (failOpen) {
            if (logFailure) {
                logger.warn(
                        "Rate limit check failed, allowing request (Fail-Open). Request={}",
                        request.getRequestURI(),
                        e
                );
            }

            metricsCollector.recordFailOpen(ruleOpt.orElse(null));
            filterChain.doFilter(request, response);
        } else {
            if (logFailure) {
                logger.error(
                        "Rate limit check failed, blocking request (Fail-Closed). Request={}",
                        request.getRequestURI(),
                        e
                );
            }

            sendServiceUnavailableResponse(response);
        }
    }

    /**
     * 503 응답 전송
     */
    private void sendServiceUnavailableResponse(HttpServletResponse response) {
        try {
            response.sendError(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Service temporarily unavailable"
            );
        } catch (IOException ioException) {
            logger.error("Failed to send error response", ioException);
            throw new RuntimeException(ioException);
        }
    }

    /**
     * 필터 적용 여부 결정 (확장 포인트)
     */
    protected boolean shouldApplyFilter(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
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
