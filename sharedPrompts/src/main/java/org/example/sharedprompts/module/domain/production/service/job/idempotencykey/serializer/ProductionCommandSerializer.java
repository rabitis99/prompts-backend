package org.example.sharedprompts.module.domain.production.service.job.idempotencykey.serializer;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.service.job.idempotencykey.exception.IdempotencyKeyGenerationException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * ProductionCommand 직렬화 전용 컴포넌트
 * 일관된 JSON 직렬화를 보장하기 위해 속성 정렬 활성화
 */
@Component
@RequiredArgsConstructor
public class ProductionCommandSerializer {

    private final ObjectMapper objectMapper;
    private ObjectMapper sortedObjectMapper;

    @PostConstruct
    @SuppressWarnings("deprecation")
    void init() {
        sortedObjectMapper = objectMapper.copy();
        // Note: configure() is deprecated but necessary for compatibility
        // Alternative would require creating new ObjectMapper which loses existing configuration
        sortedObjectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    /**
     * ProductionCommand를 JSON 문자열로 직렬화
     * 속성은 알파벳 순서로 정렬되어 일관성 보장
     */
    public String serialize(ProductionCommand command) {
        try {
            return sortedObjectMapper.writeValueAsString(command);
        } catch (Exception e) {
            throw new IdempotencyKeyGenerationException(
                    "Failed to serialize ProductionCommand: " + e.getMessage(), e);
        }
    }
}

