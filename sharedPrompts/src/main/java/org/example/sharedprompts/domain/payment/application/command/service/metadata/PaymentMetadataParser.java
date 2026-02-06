package org.example.sharedprompts.domain.payment.application.command.service.metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMetadataParser {

    private final ObjectMapper objectMapper;
    private static final TypeReference<Map<String, Object>> METADATA_TYPE_REF = 
            new TypeReference<Map<String, Object>>() {};

    public Optional<Map<String, Object>> parseMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> metadataMap = objectMapper.readValue(metadata, METADATA_TYPE_REF);
            return Optional.of(metadataMap);
        } catch (JsonProcessingException e) {
            log.debug("메타데이터 파싱 실패: metadata={}, error={}", metadata, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("메타데이터 파싱 중 예상치 못한 오류: metadata={}, error={}", 
                    metadata, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public String serializeMetadata(Map<String, Object> metadataMap) {
        try {
            return objectMapper.writeValueAsString(metadataMap);
        } catch (JsonProcessingException e) {
            log.error("메타데이터 직렬화 실패: metadataMap={}, error={}", 
                    metadataMap, e.getMessage(), e);
            throw new MetadataSerializationException("메타데이터 직렬화 실패", e);
        }
    }
}

