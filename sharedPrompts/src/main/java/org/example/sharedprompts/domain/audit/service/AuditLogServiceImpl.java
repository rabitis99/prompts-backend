package org.example.sharedprompts.domain.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.audit.AuditLog;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.domain.audit.repository.AuditLogRepository;
import org.example.sharedprompts.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog createLog(
            User actor,
            AuditEntityType entityType,
            Long entityId,
            AuditAction action,
            String description,
            String beforeState,
            String afterState,
            String ipAddress,
            String userAgent
    ) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .actor(actor)
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .description(description)
                    .beforeState(beforeState)
                    .afterState(afterState)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            AuditLog saved = auditLogRepository.save(auditLog);
            log.debug("감사 로그 생성: action={}, entityType={}, entityId={}, actorId={}",
                    action, entityType, entityId, actor.getId());

            return saved;
        } catch (Exception e) {
            // 감사 로그 기록 실패는 메인 트랜잭션에 영향을 주지 않도록 처리
            log.error("감사 로그 생성 실패: action={}, entityType={}, entityId={}, actorId={}",
                    action, entityType, entityId, actor != null ? actor.getId() : null, e);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByActor(Long actorId, Pageable pageable) {
        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByEntity(AuditEntityType entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAdminActivities(Pageable pageable) {
        return auditLogRepository.findAdminActivities(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsWithFilters(
            Long actorId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        return auditLogRepository.findWithFilters(actorId, entityType, action, startDate, endDate, pageable);
    }
}

