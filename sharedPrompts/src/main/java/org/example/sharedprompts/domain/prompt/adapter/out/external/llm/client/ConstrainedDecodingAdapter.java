package org.example.sharedprompts.domain.prompt.adapter.out.external.llm.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.out.llm.ConstrainedDecodingPort;
import org.example.sharedprompts.domain.prompt.domain.model.contract.OutputContract;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.prompt.ConstrainedPromptBuilder;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.validation.JsonSchemaValidator;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.monitoring.ComplianceTracker;
import org.example.sharedprompts.global.google.gemini.SyncGoogleGeminiClient;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * JSON 출력 형식을 강제하는 LLM 어댑터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConstrainedDecodingAdapter implements ConstrainedDecodingPort {

    private final SyncGoogleGeminiClient syncGoogleGeminiClient;
    private final ConstrainedPromptBuilder constrainedPromptBuilder;
    private final JsonSchemaValidator jsonSchemaValidator;
    private final ComplianceTracker complianceTracker;

    @Override
    public String generateConstrained(String prompt, OutputContract outputContract) {
        Objects.requireNonNull(outputContract, "outputContract must not be null");

        // JSON 형식 강제 프롬프트 생성
        String constrainedPrompt = constrainedPromptBuilder.build(prompt, outputContract);
        log.debug("[ConstrainedDecoding] JSON 강제 프롬프트 생성: format={}", outputContract.getFormat());

        // LLM 호출
        String response = syncGoogleGeminiClient.chatSync(constrainedPrompt);

        // 응답 검증
        boolean compliant = validate(response, outputContract);

        if (outputContract.hasJsonSchema()) {
            // JSON Schema 준수율 기록
            complianceTracker.record(compliant);

            if (!compliant) {
                log.warn("[ConstrainedDecoding] Schema 미준수 응답 감지: complianceRate={}",
                        complianceTracker.currentRate());
            }
        }

        return response;
    }

    @Override
    public boolean validate(String output, OutputContract outputContract) {
        Objects.requireNonNull(outputContract, "outputContract must not be null");

        // 빈 응답 방지
        if (output == null || output.isBlank()) return false;

        // 스키마가 없으면 검증 생략
        if (!outputContract.hasJsonSchema()) return true;

        // JSON Schema 검증
        return jsonSchemaValidator.isValid(output, outputContract.getJsonSchema());
    }
}