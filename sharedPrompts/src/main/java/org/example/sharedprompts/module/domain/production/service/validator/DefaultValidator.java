package org.example.sharedprompts.module.domain.production.service.validator;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.exception.ParseException;
import org.springframework.stereotype.Component;

/**
 * 기본 검증: 최소한 content 필드가 있어야 함
 */
@Component
@Slf4j
public class DefaultValidator implements ResponseValidator {

    @Override
    public void validate(JsonNode jsonNode) {
        if (!jsonNode.has("content")) {
            throw new ParseException("Missing required field: content");
        }
    }
}

