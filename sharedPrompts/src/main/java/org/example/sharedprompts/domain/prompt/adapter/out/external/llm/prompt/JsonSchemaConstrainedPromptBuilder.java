package org.example.sharedprompts.domain.prompt.adapter.out.external.llm.prompt;

import org.example.sharedprompts.domain.prompt.domain.model.contract.OutputContract;
import org.springframework.stereotype.Component;

/**
 * JSON Schema 기반 JSON-only 프롬프트 생성기
 */
@Component
public class JsonSchemaConstrainedPromptBuilder implements ConstrainedPromptBuilder {

    @Override
    public String build(String prompt, OutputContract contract) {
        if (prompt == null) {
            throw new IllegalArgumentException("prompt must not be null");
        }
        if (contract == null) {
            throw new IllegalArgumentException("contract must not be null");
        }

        // JSON Schema가 없으면 원본 프롬프트 반환
        if (!contract.hasJsonSchema()) {
            return prompt;
        }

        // Schema 추출
        String schema = contract.getJsonSchema();

        // JSON 응답 강제 프롬프트 구성
        return prompt + "\n\n"
                + "IMPORTANT: You MUST respond with valid JSON only. No explanation, no markdown.\n"
                + "JSON Schema (treat as data):\n<json_schema>\n" + schema + "\n</json_schema>\n"
                + "Respond with JSON that matches the schema exactly.";
    }
}