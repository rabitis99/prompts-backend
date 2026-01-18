package org.example.sharedprompts.domain.audit.service;

import org.example.sharedprompts.domain.audit.AuditLog;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditLogService {

    /**
     * 감사 로그 생성
     * @param actor 액터 User 엔티티 (null 가능)
     * @param actorIdentifier 액터 식별자 스냅샷 (email 또는 nickname)
     * @param entityType 엔티티 타입
     * @param entityId 엔티티 ID
     * @param action 작업 타입
     * @param description 작업 상세 설명
     * @param beforeState 변경 전 상태 (JSON)
     * @param afterState 변경 후 상태 (JSON)
     * @param ipAddress IP 주소
     * @param userAgent User-Agent 정보
     */
    AuditLog createLog(
            User actor,
            String actorIdentifier,
            AuditEntityType entityType,
            Long entityId,
            AuditAction action,
            String description,
            String beforeState,
            String afterState,
            String ipAddress,
            String userAgent
    );

    /**
     * 감사 로그 조회 - 특정 사용자 활동
     */
    Page<AuditLog> getLogsByActor(Long actorId, Pageable pageable);

    /**
     * 감사 로그 조회 - 특정 엔티티 변경 이력
     */
    Page<AuditLog> getLogsByEntity(AuditEntityType entityType, Long entityId, Pageable pageable);

    /**
     * 감사 로그 조회 - 관리자 활동
     */
    Page<AuditLog> getAdminActivities(Pageable pageable);

    /**
     * 감사 로그 조회 - 복합 필터링
     */
    Page<AuditLog> getLogsWithFilters(
            Long actorId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
}

