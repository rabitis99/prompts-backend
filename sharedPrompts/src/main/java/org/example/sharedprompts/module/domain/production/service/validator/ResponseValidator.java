package org.example.sharedprompts.module.domain.production.service.validator;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 응답 검증 인터페이스
 */
public interface ResponseValidator {
    /**
     * JSON 노드 검증
     */
    void validate(JsonNode jsonNode);
}

