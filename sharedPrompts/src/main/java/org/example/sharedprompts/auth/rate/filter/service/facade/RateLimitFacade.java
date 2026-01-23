package org.example.sharedprompts.auth.rate.filter.service.facade;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.filter.handler.RateLimitExceededFacade;
import org.example.sharedprompts.auth.rate.filter.processor.RateLimitProcessor;
import org.example.sharedprompts.auth.rate.filter.service.check.RateLimitCheckService;
import org.example.sharedprompts.auth.rate.filter.service.logging.RateLimitLoggingService;
import org.example.sharedprompts.auth.rate.filter.service.resolution.RateLimitRuleResolutionService;
import org.springframework.stereotype.Component;

/**
 * Rate Limit Facade
 * 
 * Rate Limit 관련 서비스들을 통합하여 제공하는 Facade입니다.
 * 필터의 의존성을 단순화합니다.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFacade {

    private final RateLimitProcessor processor;
    private final RateLimitCheckService checkService;
    private final RateLimitExceededFacade exceededFacade;
    private final RateLimitLoggingService loggingService;
    private final RateLimitRuleResolutionService ruleResolutionService;

    public RateLimitProcessor getProcessor() {
        return processor;
    }

    public RateLimitCheckService getCheckService() {
        return checkService;
    }

    public RateLimitExceededFacade getExceededFacade() {
        return exceededFacade;
    }

    public RateLimitLoggingService getLoggingService() {
        return loggingService;
    }

    public RateLimitRuleResolutionService getRuleResolutionService() {
        return ruleResolutionService;
    }
}




