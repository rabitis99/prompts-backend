package org.example.sharedprompts.domain.audit.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.audit.event.AuditEvent;
import org.example.sharedprompts.domain.audit.service.AuditLogService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 감사 로그 이벤트를 처리하는 리스너
 * AFTER_COMMIT 페이즈에서 처리하여 메인 트랜잭션이 완료된 후에만 로그를 저장합니다.
 * 
 * 이벤트에는 스냅샷 값(actorId, actorIdentifier)만 포함되므로,
 * 여기서 actorId로 User 엔티티를 조회하여 AuditLogService에 전달합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuditEvent(AuditEvent event) {
        try {
            // 이벤트에서 actorId로 User 엔티티 조회
            User actor = null;
            if (event.getActorId() != null) {
                actor = userRepository.findById(event.getActorId()).orElse(null);
                if (actor == null) {
                    log.warn("감사 로그 이벤트 처리: actor를 찾을 수 없음. actorId={}", event.getActorId());
                    // actor가 없어도 로그는 기록 (actorId와 actorIdentifier 스냅샷 값이 있음)
                }
            }

            auditLogService.createLog(
                    actor,
                    event.getActorIdentifier(),
                    event.getEntityType(),
                    event.getEntityId(),
                    event.getAction(),
                    event.getDescription(),
                    event.getBeforeState(),
                    event.getAfterState(),
                    event.getIpAddress(),
                    event.getUserAgent()
            );
        } catch (Exception e) {
            // 감사 로그 기록 실패는 메인 트랜잭션에 영향을 주지 않도록 처리
            log.error("감사 로그 이벤트 처리 실패: actorId={}, entityType={}, action={}, entityId={}",
                    event.getActorId(), event.getEntityType(), event.getAction(), event.getEntityId(), e);
        }
    }
}

