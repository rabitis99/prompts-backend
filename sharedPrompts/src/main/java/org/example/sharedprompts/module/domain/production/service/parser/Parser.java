package org.example.sharedprompts.module.domain.production.service.parser;

import org.example.sharedprompts.module.domain.production.service.validator.ResponseValidator;

/**
 * 파싱 전략 인터페이스
 */
public interface Parser {
    /**
     * 원본 응답을 파싱하고 검증
     */
    ParsedResponse parse(String rawResponse, ResponseValidator validator);
}

