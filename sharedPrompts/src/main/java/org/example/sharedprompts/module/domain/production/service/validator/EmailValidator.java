package org.example.sharedprompts.module.domain.production.service.validator;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.exception.ParseException;
import org.springframework.stereotype.Component;

/**
 * Email CommandType별 필수 필드 검증
 */
@Component
@Slf4j
public class EmailValidator implements ResponseValidator {

    @Override
    public void validate(JsonNode jsonNode) {
        if (!jsonNode.has("subject")) {
            throw new ParseException("Missing required field: subject");
        }
        if (!jsonNode.has("body")) {
            throw new ParseException("Missing required field: body");
        }
    }
}

