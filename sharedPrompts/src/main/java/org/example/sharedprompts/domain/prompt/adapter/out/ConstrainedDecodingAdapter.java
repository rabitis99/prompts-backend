package org.example.sharedprompts.domain.prompt.adapter.out;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.out.ConstrainedDecodingPort;
import org.example.sharedprompts.domain.prompt.domain.model.OutputContract;
import org.example.sharedprompts.global.google.gemini.SyncGoogleGeminiClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Constrained Decoding 어댑터 — 현재는 JSON 형식 강제를 프롬프트 레벨에서 구현한 스텁.
 *
 * <p>실제 벤더 Constrained Decoding(JSON Mode, Guided Decoding 등)은 벤더 API 지원 시
 * 이 클래스에서만 교체하면 된다.
 *
 * <p>compliance rate 지표는 내부적으로 추적하며, 임계값(90%) 이하 시 알림을 발송해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConstrainedDecodingAdapter implements ConstrainedDecodingPort {

    private final SyncGoogleGeminiClient syncGoogleGeminiClient;

    // 내부 compliance rate 추적용 (실제 운영에서는 메트릭 시스템 연동)
    // AtomicReference: EMA 업데이트가 read-modify-write이므로 volatile double은 원자성 미보장
    private final AtomicReference<Double> complianceRate = new AtomicReference<>(1.0);

    @Override
    public String generateConstrained(String prompt, OutputContract outputContract) {
        // JSON 형식 강제: 프롬프트에 JSON 출력 지시 추가
        String constrainedPrompt = buildConstrainedPrompt(prompt, outputContract);
        log.debug("[ConstrainedDecoding] JSON 강제 프롬프트 생성: format={}", outputContract.getFormat());

        String response = syncGoogleGeminiClient.chatSync(constrainedPrompt);

        // compliance 검증 및 rate 추적
        boolean compliant = validate(response, outputContract);
        updateComplianceRate(compliant);

        if (!compliant) {
            log.warn("[ConstrainedDecoding] Schema 미준수 응답 감지: complianceRate={}", complianceRate);
        }

        return response;
    }

    @Override
    public boolean validate(String output, OutputContract outputContract) {
        if (output == null || output.isBlank()) return false;
        if (!outputContract.hasJsonSchema()) return true;

        String trimmed = output.trim();
        // 기본 JSON 구조 검증 (실제 JSON Schema 검증은 JSON Schema 라이브러리 연동 필요)
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    @Override
    public double getComplianceRate() {
        return complianceRate.get();
    }

    private String buildConstrainedPrompt(String prompt, OutputContract contract) {
        return prompt + "\n\n"
                + "IMPORTANT: You MUST respond with valid JSON only. No explanation, no markdown.\n"
                + "JSON Schema:\n```json\n" + contract.getJsonSchema() + "\n```\n"
                + "Respond with JSON that matches the schema exactly.";
    }

    private void updateComplianceRate(boolean compliant) {
        // 지수 이동 평균 (EMA) 방식으로 compliance rate 추적 (원자적 업데이트)
        double current = complianceRate.updateAndGet(
                rate -> 0.95 * rate + 0.05 * (compliant ? 1.0 : 0.0));
        if (current < 0.9) {
            log.error("[ConstrainedDecoding] compliance rate 임계값 위반: rate={}. 대체 adapter 검토 필요",
                    current);
        }
    }
}
