package org.example.sharedprompts.auth.rate.filter.service.resolution;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.example.sharedprompts.auth.rate.filter.service.logging.RateLimitLoggingService;
import org.example.sharedprompts.auth.rate.filter.service.validation.RateLimitRequestValidator;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Rate Limit 규칙 해석 서비스
 * 
 * 요청을 검증하고 Rate Limit 규칙을 결정하는 서비스입니다.
 * 검증과 규칙 해석을 명확히 분리했습니다.
 */
@Service
@RequiredArgsConstructor
public class RateLimitRuleResolutionService {

    private final RateLimitRequestValidator validator;
    private final RateLimitRuleResolverService ruleResolver;
    private final RateLimitLoggingService loggingService;

    /**
     * 요청을 검증하고 Rate Limit 규칙을 결정합니다.
     * 
     * @param request HttpServletRequest
     * @return Optional<RateLimitRule> (적용할 규칙이 없으면 empty)
     */
    public Optional<RateLimitRule> resolveRule(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 요청 검증
        if (!validator.shouldApply(uri)) {
            return Optional.empty();
        }

        // HTTP Method 파싱
        Optional<HttpMethod> methodOpt = validator.parseHttpMethod(request);
        if (methodOpt.isEmpty()) {
            loggingService.logInvalidHttpMethod(request.getMethod());
            return Optional.empty();
        }

        // Rate Limit 규칙 결정
        return ruleResolver.resolve(uri, methodOpt.get());
    }
}






