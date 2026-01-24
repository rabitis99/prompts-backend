package org.example.sharedprompts.auth.rate.filter.processor;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.filter.metrics.RateLimitMetricsCollector;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitCheckResult;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitFilterContext;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitKey;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitResultWithKey;
import org.example.sharedprompts.auth.rate.filter.service.check.RateLimitCheckService;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

/**
 * Rate Limit 처리기
 * 
 * Rate Limit 규칙 처리 로직을 담당합니다.
 * 순수 도메인 로직만 처리하고, HTTP 관련 처리는 Filter에서 담당합니다.
 */
@Component
@RequiredArgsConstructor
public class RateLimitProcessor {

    private final RateLimitCheckService checkService;
    private final RateLimitMetricsCollector metricsCollector;

    /**
     * Rate Limit 규칙을 처리하고 결과를 반환합니다.
     * 
     * @param rule RateLimitRule
     * @param context RateLimitFilterContext
     * @param buildKeyFunction 키 생성 함수
     * @return RateLimitResultWithKey (키 생성 실패 시 empty)
     */
    public Optional<RateLimitResultWithKey> processRule(
            RateLimitRule rule,
            RateLimitFilterContext context,
            Function<RateLimitRule, Optional<RateLimitKey>> buildKeyFunction
    ) {
        // Rate Limit 키 생성
        Optional<RateLimitKey> keyOpt = buildKeyFunction.apply(rule);
        if (keyOpt.isEmpty()) {
            return Optional.empty();
        }

        RateLimitKey key = keyOpt.get();
        
        // Rate Limit 체크 (key.value()를 사용하여 Redis 키로 사용)
        RateLimitCheckResult checkResult = checkService.check(key.value(), rule);
        
        // 메트릭 수집 (Observer 패턴) - 성공한 체크에 대해서만
        if (!checkResult.isFailed()) {
            metricsCollector.recordCheck(rule, checkResult.getResult());
        }

        return Optional.of(RateLimitResultWithKey.of(rule, key, checkResult));
    }
}

