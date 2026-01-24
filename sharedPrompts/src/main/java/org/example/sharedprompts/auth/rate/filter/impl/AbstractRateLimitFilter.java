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
        try (ScopedResponseHandled ignored = new ScopedResponseHandled()) {

            if (!shouldApplyFilter(request, response, filterChain)) {
                filterChain.doFilter(request, response);
                return;
            }

            RateLimitFilterContext context = createContext(request);
            processRateLimitCheck(context, request, response, filterChain);
        }
    }

    private void handleRateLimitResult(
            RateLimitResultWithKey result,
            RateLimitFilterContext context,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (result.isExceeded()) {
            // Rate limit 초과 로깅은 이미 logRateLimitExceeded()에서 처리되었으므로,
            // handle() 메서드의 logCallback에는 빈 람다를 전달합니다.
            // 이는 중복 로깅을 방지하기 위한 설계입니다.
            // logCallback은 handle() 메서드 내부에서 recordLog()를 통해 호출되며,
            // 필요시 추가적인 로깅이나 후처리를 수행할 수 있는 확장 포인트 역할을 합니다.
            logRateLimitExceeded(result.getRule(), result.getKey(), result.getResult(), context, request);
            facade.getExceededFacade().handle(result.getRule(), result.getKey(), result.getResult(), response, (k, r) -> {});
        } else {
            filterChain.doFilter(request, response);
        }
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
        Optional<RateLimitResultWithKey> opt;
        try {
            opt = facade.getRuleResolutionService().resolveRule(request)
                    .flatMap(rule -> keyStrategy.buildKey(rule, context)
                            .map(key -> new RateLimitResultWrapper(rule, key)))
                    .flatMap(wrapper -> facade.getProcessor()
                            .processRule(wrapper.rule(), context, r -> Optional.of(wrapper.key())));
        } catch (Exception e) {
            handleRateLimitCheckFailure(e, filterChain, request, response);
            return;
        }

        if (opt.isPresent()) {
            handleRateLimitResult(opt.get(), context, request, response, filterChain);
        } else {
            filterChain.doFilter(request, response);
        }
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

    /**
     * ThreadLocal 기반 플래그를 Scoped 방식으로 처리
     */
    private static final class ScopedResponseHandled implements AutoCloseable {
        private static final ThreadLocal<Boolean> RESPONSE_HANDLED = ThreadLocal.withInitial(() -> false);

        ScopedResponseHandled() {
            RESPONSE_HANDLED.set(false);
        }

        static boolean isHandled() {
            return RESPONSE_HANDLED.get();
        }

        static void markHandled() {
            RESPONSE_HANDLED.set(true);
        }

        @Override
        public void close() {
            RESPONSE_HANDLED.remove();
        }
    }

    /**
     * Rule-Key 묶음 객체
     */
    private record RateLimitResultWrapper(RateLimitRule rule, RateLimitKey key) {}
}
