package org.example.sharedprompts.domain.prompt.adapter.out.external.llm.validation;

/**
 * JSON Schema 검증 인터페이스
 */
public interface JsonSchemaValidator {

    /**
     * JSON이 Schema를 만족하는지 검증
     */
    boolean isValid(String json, String schemaJson);
}