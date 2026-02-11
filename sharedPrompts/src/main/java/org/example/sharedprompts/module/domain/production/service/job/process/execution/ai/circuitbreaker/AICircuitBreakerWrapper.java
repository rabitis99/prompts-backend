package org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentResult;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.AIServiceException;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class AICircuitBreakerWrapper {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public AIContentResult execute(String circuitBreakerName, Supplier<AIContentResult> supplier) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);

        try {
            return circuitBreaker.executeSupplier(supplier);
        } catch (CallNotPermittedException e) {
            log.warn("AI Circuit Breaker is OPEN, rejecting call", e);
            return AIContentResult.failure("AI service is temporarily unavailable (Circuit Breaker Open)");
        } catch (Exception e) {
            log.error("AI call failed with Circuit Breaker protection", e);
            throw new AIServiceException("AI call failed", e);
        }
    }
}

