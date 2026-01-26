package org.example.sharedprompts.domain.audit.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.domain.audit.event.AuditEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

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
    private final ObjectMapper objectMapper;

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
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("감사 로그 {} 직렬화 실패: {}", label, e.getMessage());
            return null;
        }
    }
}

