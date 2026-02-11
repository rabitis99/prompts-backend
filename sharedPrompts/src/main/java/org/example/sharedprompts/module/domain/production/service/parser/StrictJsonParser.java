package org.example.sharedprompts.module.domain.production.service.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.exception.ParseException;
import org.example.sharedprompts.module.domain.production.service.validator.ResponseValidator;
import org.springframework.stereotype.Component;

/**
 * Strict JSON 파싱 전략
 * ObjectMapper를 사용해 JSON strict parsing 수행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StrictJsonParser implements Parser {

    private final ObjectMapper objectMapper;

    @Override
    public ParsedResponse parse(String rawResponse, ResponseValidator validator) {
        try {
            JsonNode jsonNode = objectMapper.readTree(rawResponse);
            validator.validate(jsonNode);
            String parsedJson = objectMapper.writeValueAsString(jsonNode);
            return new ParsedResponse(parsedJson, jsonNode);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new ParseException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }
}

