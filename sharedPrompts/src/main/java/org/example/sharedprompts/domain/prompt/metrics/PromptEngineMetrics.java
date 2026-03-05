package org.example.sharedprompts.domain.prompt.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 프롬프트 엔진 메트릭 수집기.
 */
@Component
@RequiredArgsConstructor
public class PromptEngineMetrics {

    private final MeterRegistry meterRegistry;

    private Counter successCounter(EngineMode effectiveMode) {
        return Counter.builder("prompt.generate.success")
                .tag("engine_mode", effectiveMode.name())
                .description("프롬프트 생성 성공 횟수")
                .register(meterRegistry);
    }

    private Counter failureCounter(String reason, EngineMode effectiveMode) {
        return Counter.builder("prompt.generate.failure")
                .tag("reason", reason)
                .tag("engine_mode", effectiveMode.name())
                .description("프롬프트 생성 실패 횟수")
                .register(meterRegistry);
    }

    private Counter schemaContractFailureCounter() {
        return Counter.builder("prompt.generate.schema_contract_fail")
                .description("JSON 스키마 계약 실패 횟수")
                .register(meterRegistry);
    }

    private Counter verifyFailureCounter() {
        return Counter.builder("prompt.generate.verify_fail")
                .description("검증 실패 횟수")
                .register(meterRegistry);
    }

    private Counter repairCountCounter(int repairCount) {
        return Counter.builder("prompt.generate.repair_count")
                .tag("count", String.valueOf(repairCount))
                .description("Repair 시도 횟수 분포")
                .register(meterRegistry);
    }

    private Timer latencyTimer(EngineMode effectiveMode) {
        return Timer.builder("prompt.generate.latency")
                .tag("engine_mode", effectiveMode.name())
                .description("프롬프트 생성 지연 시간")
                .register(meterRegistry);
    }

    public void recordSuccess(EngineMode effectiveMode, long latencyMs, int repairCount, boolean verifyPassed, boolean schemaContractFailed) {
        successCounter(effectiveMode).increment();
        latencyTimer(effectiveMode).record(latencyMs, TimeUnit.MILLISECONDS);
        repairCountCounter(repairCount).increment();
        if (!verifyPassed) {
            verifyFailureCounter().increment();
        }
        if (schemaContractFailed) {
            schemaContractFailureCounter().increment();
        }
    }

    public void recordFailure(EngineMode effectiveMode, long latencyMs, String reason) {
        failureCounter(reason != null ? reason : "unknown", effectiveMode).increment();
        latencyTimer(effectiveMode).record(latencyMs, TimeUnit.MILLISECONDS);
    }
}

