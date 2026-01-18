package org.example.sharedprompts.domain.audit.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.domain.audit.event.AuditEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 감사 로그 이벤트를 발행하는 유틸리티 클래스
 * 
 * JPA Entity를 이벤트에 포함하지 않고 스냅샷 값(actorId, actorIdentifier)만 전달합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private static volatile ObjectMapper objectMapper;

    /**
     * Spring 관리 ObjectMapper 설정
     * 별도 설정 클래스에서 이 메서드를 호출하여 Spring 관리 ObjectMapper를 주입할 수 있습니다.
     * 
     * @param mapper Spring 관리 ObjectMapper
     */
    public static void setObjectMapper(ObjectMapper mapper) {
        objectMapper = mapper;
    }

    /**
     * ObjectMapper 조회
     * Spring 관리 ObjectMapper가 설정되어 있으면 사용하고, 없으면 기본 ObjectMapper를 생성합니다.
     */
    private static ObjectMapper getObjectMapper() {
        return objectMapper != null ? objectMapper : createObjectMapper();
    }

    /**
     * Spring의 JacksonConfig와 동일한 설정을 가진 ObjectMapper 생성
     * 응답 형식 일관성을 보장하기 위해 동일한 설정을 적용합니다.
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // JavaTimeModule 등록 (LocalDateTime 직렬화/역직렬화)
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        mapper.registerModule(javaTimeModule);

        // Spring JacksonConfig와 동일한 설정 적용
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper;
    }

    /**
     * 감사 로그 이벤트 발행
     * 
     * @param actorId 액터 사용자 ID
     * @param actorIdentifier 액터 식별자 (email 또는 nickname)
     */
    public void publish(
            Long actorId,
            String actorIdentifier,
            AuditEntityType entityType,
            Long entityId,
            AuditAction action,
            String description,
            Object beforeState,
            Object afterState,
            String ipAddress,
            String userAgent
    ) {
        try {
            String beforeStateJson = safeSerialize(beforeState, "beforeState");
            String afterStateJson = safeSerialize(afterState, "afterState");

            AuditEvent event = AuditEvent.builder()
                    .actorId(actorId)
                    .actorIdentifier(actorIdentifier)
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .description(description)
                    .beforeState(beforeStateJson)
                    .afterState(afterStateJson)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("감사 로그 이벤트 발행 실패: action={}, entityType={}, entityId={}, actorId={}",
                    action, entityType, entityId, actorId, e);
        }
    }

    /**
     * 안전한 직렬화: 실패 시 null 반환하여 이벤트 발행은 계속 진행
     */
    private String safeSerialize(Object value, String label) {
        if (value == null) {
            return null;
        }
        try {
            return getObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            log.warn("감사 로그 {} 직렬화 실패: {}", label, e.getMessage());
            return null;
        }
    }
}

