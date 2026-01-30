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
import org.example.sharedprompts.auth.rate.filter.util.RateLimitHeaderUtil;
import org.example.sharedprompts.auth.rate.policy.RateLimitProperties;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Rate Limit 필터 추상 클래스 (Template Method 패턴)
 */
public abstract class AbstractRateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRateLimitFilter.class);
    
    /**
     * 재진입 방지를 위한 request attribute 키
     */
    private static final String FILTER_PROCESSED_ATTRIBUTE = 
            AbstractRateLimitFilter.class.getName() + ".PROCESSED";

    protected final RateLimitFacade facade;
    protected final RateLimitKeyStrategy keyStrategy;
    protected final RateLimitProperties rateLimitProperties;
    protected final RateLimitMetricsCollector metricsCollector;
    @Nullable
    protected final RedisTemplate<String, Object> redisTemplate;

    protected AbstractRateLimitFilter(
            RateLimitFacade facade,
            RateLimitKeyStrategy keyStrategy,
            RateLimitProperties rateLimitProperties,
            RateLimitMetricsCollector metricsCollector,
            @Nullable RedisTemplate<String, Object> redisTemplate
    ) {
        this.facade = facade;
        this.keyStrategy = keyStrategy;
        this.rateLimitProperties = rateLimitProperties;
        this.metricsCollector = metricsCollector;
        this.redisTemplate = redisTemplate;
    }

    protected AbstractRateLimitFilter(
            RateLimitFacade facade,
            RateLimitKeyStrategy keyStrategy,
            RateLimitProperties rateLimitProperties,
            RateLimitMetricsCollector metricsCollector
    ) {
        this(facade, keyStrategy, rateLimitProperties, metricsCollector, null);
    }

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 재진입 가드: 동일 요청에 대해 필터가 중복 실행되는 것을 방지
        if (request.getAttribute(FILTER_PROCESSED_ATTRIBUTE) != null) {
            filterChain.doFilter(request, response);
            return;
        }
        request.setAttribute(FILTER_PROCESSED_ATTRIBUTE, Boolean.TRUE);

        if (!shouldApplyFilter(request, response, filterChain)) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitFilterContext context = createContext(request);
        boolean shouldProceed = processRateLimitCheck(context, request, response);
        
        // filterChain.doFilter()는 최상위에서 단 1회만 호출
        if (shouldProceed) {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Rate limit 체크 결과를 처리합니다.
     * 
     * @return true면 filterChain.doFilter()를 호출해야 함, false면 이미 응답 작성 완료
     */
    private boolean handleRateLimitResult(
            RateLimitResultWithKey result,
            RateLimitFilterContext context,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        if (result.isExceeded()) {
            handleRateLimitExceeded(result, context, request, response);
            return false; // 응답 작성 완료, filterChain.doFilter() 호출 불필요
        } else {
            // 성공 응답에도 RateLimit 헤더 추가 (요구사항)
            addRateLimitHeaders(response, result);
            return true; // filterChain.doFilter() 호출 필요
        }
    }

    /**
     * 성공 응답에 RateLimit 헤더를 추가합니다.
     * 
     * @param response HttpServletResponse
     * @param result RateLimitResultWithKey
     */
    private void addRateLimitHeaders(
            HttpServletResponse response,
            RateLimitResultWithKey result
    ) {
        // Redis 키를 사용하여 정확한 TTL 조회
        String rateLimitKey = result.getKey() != null ? result.getKey().value() : null;
        RateLimitHeaderUtil.addRateLimitHeaders(
                response,
                result.getRule(),
                result.getResult(),
                redisTemplate,
                rateLimitKey
        );
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
        // Redis 키를 사용하여 정확한 TTL 조회
        String rateLimitKey = result.getKey() != null ? result.getKey().value() : null;
        facade.getExceededFacade().handle(
                result.getRule(),
                result.getKey(),
                result.getResult(),
                response,
                (k, r) -> {},
                null, // errorCode
                redisTemplate,
                rateLimitKey
        );
    }

    /**
     * Rate limit 체크 메인 흐름
     * 
     * @return true면 filterChain.doFilter()를 호출해야 함, false면 이미 응답 작성 완료
     */
    private boolean processRateLimitCheck(
            RateLimitFilterContext context,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        Optional<RateLimitResultWithKey> result;
        try {
            result = performRateLimitCheck(context, request);
        } catch (Exception e) {
            // Redis 등 인프라 장애 상황만 정책적으로 처리됨을 전제로 함
            return handleRateLimitCheckFailure(e, request, response);
        }

        if (result.isPresent()) {
            return handleRateLimitResult(result.get(), context, request, response);
        } else {
            return true; // 규칙이 없으면 통과, filterChain.doFilter() 호출 필요
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
     * 
     * @return true면 filterChain.doFilter()를 호출해야 함, false면 이미 응답 작성 완료
     */
    private boolean handleRateLimitCheckFailure(
            Exception e,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

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
            return true; // Fail-Open이면 통과, filterChain.doFilter() 호출 필요
        } else {
            if (logFailure) {
                logger.error(
                        "Rate limit check failed, blocking request (Fail-Closed). Request={}",
                        request.getRequestURI(),
                        e
                );
            }

            sendServiceUnavailableResponse(response);
            return false; // 응답 작성 완료, filterChain.doFilter() 호출 불필요
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
     * 
     * 주의: 이 메서드에서는 filterChain을 사용하지 않습니다.
     * filterChain.doFilter()는 doFilterInternal 최상위에서만 호출됩니다.
     */
    protected boolean shouldApplyFilter(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) {
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
