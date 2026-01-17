package org.example.sharedprompts.domain.audit.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.audit.event.AuditEvent;
import org.example.sharedprompts.domain.audit.service.AuditLogService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 감사 로그 이벤트를 처리하는 리스너
 * 비동기로 처리하여 메인 트랜잭션에 영향을 주지 않습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogService auditLogService;

    @Async
    @EventListener
    public void handleAuditEvent(AuditEvent event) {
        auditLogService.createLog(
                event.getActor(),
                event.getEntityType(),
                event.getEntityId(),
                event.getAction(),
                event.getDescription(),
                event.getBeforeState(),
                event.getAfterState(),
                event.getIpAddress(),
                event.getUserAgent()
        );
    }
}

