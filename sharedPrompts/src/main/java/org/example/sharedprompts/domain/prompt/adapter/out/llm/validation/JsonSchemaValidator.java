package org.example.sharedprompts.domain.prompt.adapter.out.llm.validation;

/**
 * JSON 문자열과 JSON Schema 문자열을 기반으로 유효성을 검증하는 인터페이스.
 */
public interface JsonSchemaValidator {

    /**
     * 주어진 JSON 문자열이 주어진 JSON Schema를 만족하는지 여부를 반환한다.
     */
    boolean isValid(String json, String schemaJson);
}

