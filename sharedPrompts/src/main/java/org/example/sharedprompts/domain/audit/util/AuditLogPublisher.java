package org.example.sharedprompts.domain.audit.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.domain.audit.event.AuditEvent;
import org.example.sharedprompts.domain.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 감사 로그 이벤트를 발행하는 유틸리티 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 감사 로그 이벤트 발행
     */
    public void publish(
            User actor,
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
            String beforeStateJson = beforeState != null ? objectMapper.writeValueAsString(beforeState) : null;
            String afterStateJson = afterState != null ? objectMapper.writeValueAsString(afterState) : null;

            AuditEvent event = AuditEvent.builder()
                    .actor(actor)
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
            log.error("감사 로그 이벤트 발행 실패: action={}, entityType={}, entityId={}",
                    action, entityType, entityId, e);
        }
    }
}

