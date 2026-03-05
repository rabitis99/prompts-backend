package org.example.sharedprompts.domain.prompt.adapter.out.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.out.llm.ConstrainedDecodingPort;
import org.example.sharedprompts.domain.prompt.domain.model.contract.OutputContract;
import org.example.sharedprompts.domain.prompt.adapter.out.llm.prompt.ConstrainedPromptBuilder;
import org.example.sharedprompts.domain.prompt.adapter.out.llm.validation.JsonSchemaValidator;
import org.example.sharedprompts.domain.prompt.adapter.out.llm.monitoring.ComplianceTracker;
import org.example.sharedprompts.global.google.gemini.SyncGoogleGeminiClient;
import org.springframework.stereotype.Component;

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
    private final ConstrainedPromptBuilder constrainedPromptBuilder;
    private final JsonSchemaValidator jsonSchemaValidator;
    private final ComplianceTracker complianceTracker;

    @Override
    public String generateConstrained(String prompt, OutputContract outputContract) {
        String constrainedPrompt = constrainedPromptBuilder.build(prompt, outputContract);
        log.debug("[ConstrainedDecoding] JSON 강제 프롬프트 생성: format={}", outputContract.getFormat());

        String response = syncGoogleGeminiClient.chatSync(constrainedPrompt);

        boolean compliant = validate(response, outputContract);
        if (outputContract.hasJsonSchema()) {
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
        if (output == null || output.isBlank()) return false;
        if (!outputContract.hasJsonSchema()) return true;

        return jsonSchemaValidator.isValid(output, outputContract.getJsonSchema());
    }
}
