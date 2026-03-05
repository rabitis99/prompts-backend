package org.example.sharedprompts.domain.prompt.adapter.out.llm.prompt;

import org.example.sharedprompts.domain.prompt.domain.model.contract.OutputContract;
import org.springframework.stereotype.Component;

/**
 * OutputContract의 JSON Schema 정보를 사용해 JSON-only 응답을 강제하는 프롬프트를 생성한다.
 */
@Component
public class JsonSchemaConstrainedPromptBuilder implements ConstrainedPromptBuilder {

    @Override
    public String build(String prompt, OutputContract contract) {
        if (!contract.hasJsonSchema()) {
            return prompt;
        }

        return prompt + "\n\n"
                + "IMPORTANT: You MUST respond with valid JSON only. No explanation, no markdown.\n"
                + "JSON Schema:\n```json\n" + contract.getJsonSchema() + "\n```\n"
                + "Respond with JSON that matches the schema exactly.";
    }
}

