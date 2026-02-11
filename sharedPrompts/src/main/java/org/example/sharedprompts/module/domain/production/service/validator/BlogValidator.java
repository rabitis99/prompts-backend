package org.example.sharedprompts.module.domain.production.service.validator;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.exception.ParseException;
import org.springframework.stereotype.Component;

/**
 * Blog CommandType별 필수 필드 검증
 */
@Component
@Slf4j
public class BlogValidator implements ResponseValidator {

    @Override
    public void validate(JsonNode jsonNode) {
        if (!jsonNode.has("title")) {
            throw new ParseException("Missing required field: title");
        }
        if (!jsonNode.has("content")) {
            throw new ParseException("Missing required field: content");
        }
    }
}

