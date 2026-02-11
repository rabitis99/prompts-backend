package org.example.sharedprompts.module.domain.production.service.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.exception.ParseException;
import org.example.sharedprompts.module.domain.production.service.validator.ResponseValidator;
import org.springframework.stereotype.Component;

/**
 * Relaxed JSON 파싱 전략
 * ObjectMapper로 JSON 파싱, 최소 relaxed 검증 수행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RelaxedJsonParser implements Parser {

    private final ObjectMapper objectMapper;

    @Override
    public ParsedResponse parse(String rawResponse, ResponseValidator validator) {
        try {
            JsonNode jsonNode = objectMapper.readTree(rawResponse);
            
            // 최소 relaxed 검증: JSON 객체이고 비어있지 않아야 함
            if (!jsonNode.isObject()) {
                throw new ParseException("Response is not a JSON object");
            }
            if (jsonNode.isEmpty()) {
                throw new ParseException("Response is empty");
            }
            
            // Validator의 relaxed 검증 수행
            validator.validate(jsonNode);
            
            String parsedJson = objectMapper.writeValueAsString(jsonNode);
            log.info("Relaxed parsing succeeded");
            return new ParsedResponse(parsedJson, jsonNode);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new ParseException("Failed to parse JSON in relaxed mode: " + e.getMessage(), e);
        }
    }
}

