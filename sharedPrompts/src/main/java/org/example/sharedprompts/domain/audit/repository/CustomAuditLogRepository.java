package org.example.sharedprompts.domain.audit.repository;

import org.example.sharedprompts.domain.audit.AuditLog;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface CustomAuditLogRepository {
    /**
     * 관리자 활동만 조회
     */
    Page<AuditLog> findAdminActivities(Pageable pageable);

    /**
     * 복합 조건으로 감사 로그 조회 (관리자용)
     */
    Page<AuditLog> findWithFilters(
            Long actorId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
}

