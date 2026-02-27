package org.example.sharedprompts.domain.prompt.adapter.out;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.out.ConstrainedDecodingPort;
import org.example.sharedprompts.domain.prompt.domain.model.OutputContract;
import org.example.sharedprompts.global.google.gemini.SyncGoogleGeminiClient;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
            log.warn("[ConstrainedDecoding] Schema 미준수 응답 감지: complianceRate={}", complianceRate.get());
        }

        return response;
    }

    @Override
    public boolean validate(String output, OutputContract outputContract) {
        if (output == null || output.isBlank()) return false;
        if (!outputContract.hasJsonSchema()) return true;

        try {
            JsonNode outputNode = OBJECT_MAPPER.readTree(output);
            String schemaJson = outputContract.getJsonSchema();
            if (schemaJson == null || schemaJson.isBlank()) {
                return true;
            }
            JsonNode schemaNode = OBJECT_MAPPER.readTree(schemaJson);
            JsonSchema schema = JsonSchemaFactory
                    .getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(schemaNode);
            Set<ValidationMessage> errors = schema.validate(outputNode);
            if (!errors.isEmpty()) {
                String errorMessages = errors.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining(", "));
                log.debug("[ConstrainedDecoding] JSON Schema 검증 실패: {}", errorMessages);
                return false;
            }
            return true;
        } catch (JsonProcessingException e) {
            log.debug("[ConstrainedDecoding] JSON 구문/스키마 파싱 실패", e);
            return false;
        }
    }

    @Override
    public double getComplianceRate() {
        return complianceRate.get();
    }

    private String buildConstrainedPrompt(String prompt, OutputContract contract) {
        if (!contract.hasJsonSchema()) {
            return prompt;
        }

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
