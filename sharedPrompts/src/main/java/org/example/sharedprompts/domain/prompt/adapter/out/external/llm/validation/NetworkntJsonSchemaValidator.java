package org.example.sharedprompts.domain.prompt.adapter.out.external.llm.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * networknt 기반 JSON Schema 검증기
 */
@Slf4j
@Component
public class NetworkntJsonSchemaValidator implements JsonSchemaValidator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_SCHEMA_CACHE_SIZE = 1_000;
    private static final Cache<String, Optional<JsonSchema>> SCHEMA_CACHE = Caffeine.newBuilder()
            .maximumSize(MAX_SCHEMA_CACHE_SIZE)
            .build();

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

            // 스키마 캐시 조회 또는 생성
            Optional<JsonSchema> schemaOpt = SCHEMA_CACHE.get(schemaJson, key -> {
                try {
                    JsonNode schemaNode = OBJECT_MAPPER.readTree(key);
                    SpecVersion.VersionFlag version = SpecVersionDetector.detect(schemaNode);
                    JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(version);
                    return Optional.of(schemaFactory.getSchema(schemaNode));
                } catch (JsonProcessingException e) {
                    log.debug("[ConstrainedDecoding] JSON 스키마 파싱 실패", e);
                    return Optional.empty();
                }
            });

            // 유효하지 않은 스키마 차단
            if (Objects.requireNonNull(schemaOpt).isEmpty()) {
                return false;
            }

            // JSON Schema 검증
            Set<ValidationMessage> errors = schemaOpt.get().validate(outputNode);

            // 검증 실패 처리
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