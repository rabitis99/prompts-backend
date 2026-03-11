package org.example.sharedprompts.domain.prompt.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineMode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 프롬프트 엔진 메트릭 수집기.
 * <p>실패 사유(reason) 태그는 카디널리티 및 민감정보 유출 방지를 위해 화이트리스트로만 허용한다.</p>
 */
@Component
@RequiredArgsConstructor
public class PromptEngineMetrics {

    private static final Set<String> KNOWN_FAILURE_REASONS = Set.of(
            "IllegalArgumentException",
            "IllegalStateException",
            "SchemaContractViolationException",
            "ValidationException",
            "TimeoutException",
            "UnsupportedOperationException"
    );

    private final MeterRegistry meterRegistry;
    private final Map<EngineMode, Counter> successCounters = new ConcurrentHashMap<>();
    private final Map<EngineMode, Timer> latencyTimers = new ConcurrentHashMap<>();
    private final Map<EngineMode, Counter> verifyFailureCounters = new ConcurrentHashMap<>();
    private final Map<EngineMode, Counter> schemaFailureCounters = new ConcurrentHashMap<>();

    private Counter successCounter(EngineMode effectiveMode) {
        return successCounters.computeIfAbsent(effectiveMode, mode ->
                Counter.builder("prompt.generate.success")
                        .tag("engine_mode", mode.name())
                        .description("프롬프트 생성 성공 횟수")
                        .register(meterRegistry));
    }

    private Counter failureCounter(String reason, EngineMode effectiveMode) {
        return Counter.builder("prompt.generate.failure")
                .tag("reason", reason)
                .tag("engine_mode", effectiveMode.name())
                .description("프롬프트 생성 실패 횟수")
                .register(meterRegistry);
    }

    private Counter schemaContractFailureCounter(EngineMode effectiveMode) {
        return schemaFailureCounters.computeIfAbsent(effectiveMode, mode ->
                Counter.builder("prompt.generate.schema_contract_fail")
                        .tag("engine_mode", mode.name())
                        .description("JSON 스키마 계약 실패 횟수")
                        .register(meterRegistry));
    }

    private Counter verifyFailureCounter(EngineMode effectiveMode) {
        return verifyFailureCounters.computeIfAbsent(effectiveMode, mode ->
                Counter.builder("prompt.generate.verify_fail")
                        .tag("engine_mode", mode.name())
                        .description("검증 실패 횟수")
                        .register(meterRegistry));
    }

    private DistributionSummary repairCountSummary() {
        return DistributionSummary.builder("prompt.generate.repair_count")
                .description("Repair 시도 횟수 분포")
                .register(meterRegistry);
    }

    private Timer latencyTimer(EngineMode effectiveMode) {
        return latencyTimers.computeIfAbsent(effectiveMode, mode ->
                Timer.builder("prompt.generate.latency")
                        .tag("engine_mode", mode.name())
                        .description("프롬프트 생성 지연 시간")
                        .register(meterRegistry));
    }

    public void recordSuccess(EngineMode effectiveMode, long latencyMs, int repairCount, boolean verifyPassed, boolean schemaContractFailed) {
        successCounter(effectiveMode).increment();
        latencyTimer(effectiveMode).record(latencyMs, TimeUnit.MILLISECONDS);
        repairCountSummary().record(repairCount);
        if (!verifyPassed) {
            verifyFailureCounter(effectiveMode).increment();
        }
        if (schemaContractFailed) {
            schemaContractFailureCounter(effectiveMode).increment();
        }
    }

    public void recordFailure(EngineMode effectiveMode, long latencyMs, String reason) {
        String normalizedReason = (reason != null && KNOWN_FAILURE_REASONS.contains(reason)) ? reason : "unknown";
        failureCounter(normalizedReason, effectiveMode).increment();
        latencyTimer(effectiveMode).record(latencyMs, TimeUnit.MILLISECONDS);
    }
}

