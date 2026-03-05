package org.example.sharedprompts.domain.prompt.adapter.out.llm.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * networknt JSON Schema 라이브러리를 사용하는 구현체.
 */
@Slf4j
@Component
public class NetworkntJsonSchemaValidator implements JsonSchemaValidator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JsonSchemaFactory SCHEMA_FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    private static final int MAX_SCHEMA_CACHE_SIZE = 1_000;
    private static final Map<String, Optional<JsonSchema>> SCHEMA_CACHE = new ConcurrentHashMap<>();

    @Override
    public boolean isValid(String json, String schemaJson) {
        if (json == null || json.isBlank()) {
            return false;
        }
        if (schemaJson == null || schemaJson.isBlank()) {
            return true;
        }

        try {
            JsonNode outputNode = OBJECT_MAPPER.readTree(json);
            if (SCHEMA_CACHE.size() >= MAX_SCHEMA_CACHE_SIZE) {
                SCHEMA_CACHE.clear();
            }

            Optional<JsonSchema> schemaOpt = SCHEMA_CACHE.computeIfAbsent(schemaJson, key -> {
                try {
                    JsonNode schemaNode = OBJECT_MAPPER.readTree(key);
                    return Optional.of(SCHEMA_FACTORY.getSchema(schemaNode));
                } catch (JsonProcessingException e) {
                    log.debug("[ConstrainedDecoding] JSON 스키마 파싱 실패", e);
                    return Optional.empty();
                }
            });
            if (schemaOpt.isEmpty()) {
                return false;
            }
            Set<ValidationMessage> errors = schemaOpt.get().validate(outputNode);
            if (!errors.isEmpty()) {
                String errorMessages = errors.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining(", "));
                log.debug("[ConstrainedDecoding] JSON Schema 검증 실패: {}", errorMessages);
                return false;
            }
            return true;
        } catch (JsonProcessingException e) {
            log.debug("[ConstrainedDecoding] JSON 구문/스키마 파싱 실패", e);
            return false;
        }
    }
}

