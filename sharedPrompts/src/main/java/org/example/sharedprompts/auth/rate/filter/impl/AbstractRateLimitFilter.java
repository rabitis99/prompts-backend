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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Rate Limit 필터 추상 클래스 (Template Method 패턴)
 */
@RequiredArgsConstructor
public abstract class AbstractRateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRateLimitFilter.class);

    protected final RateLimitFacade facade;
    protected final RateLimitKeyStrategy keyStrategy;

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

            facade.getRuleResolutionService().resolveRule(request)
                    .flatMap(rule -> keyStrategy.buildKey(rule, context).map(key -> new RateLimitResultWrapper(rule, key)))
                    .flatMap(wrapper -> facade.getProcessor().processRule(wrapper.rule(), context, r -> Optional.of(wrapper.key())))
                    .ifPresentOrElse(
                            result -> {
                                try {
                                    handleRateLimitResult(result, context, request, response, filterChain);
                                } catch (ServletException | IOException e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            () -> safeFilterChain(filterChain, request, response)
                    );

        } catch (Exception e) {
            logger.warn("Rate limit check failed, allowing request (Fail-Open)", e);
            safeFilterChain(filterChain, request, response);
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
            logRateLimitExceeded(result.getRule(), result.getKey(), result.getResult(), context, request);
            facade.getExceededFacade().handle(result.getRule(), result.getKey(), result.getResult(), response, (k, r) -> {});
        } else {
            safeFilterChain(filterChain, request, response);
        }
    }

    private void safeFilterChain(FilterChain filterChain, HttpServletRequest request, HttpServletResponse response) {
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException e) {
            throw new RuntimeException(e);
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
