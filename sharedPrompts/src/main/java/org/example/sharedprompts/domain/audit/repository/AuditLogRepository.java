package org.example.sharedprompts.domain.audit.repository;

import org.example.sharedprompts.domain.audit.AuditLog;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 특정 사용자의 활동 로그 조회
     */
    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId, Pageable pageable);

    /**
     * 특정 엔티티의 변경 이력 조회
     */
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            AuditEntityType entityType,
            Long entityId,
            Pageable pageable
    );

    /**
     * 특정 작업 타입의 로그 조회
     */
    Page<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action, Pageable pageable);

    /**
     * 엔티티 타입과 작업 타입으로 조회
     */
    Page<AuditLog> findByEntityTypeAndActionOrderByCreatedAtDesc(
            AuditEntityType entityType,
            AuditAction action,
            Pageable pageable
    );

    /**
     * 기간별 감사 로그 조회
     */
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt >= :startDate AND a.createdAt <= :endDate ORDER BY a.createdAt DESC")
    Page<AuditLog> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 관리자 활동만 조회
     */
    @Query("SELECT a FROM AuditLog a JOIN FETCH a.actor WHERE a.actor.role = 'ROLE_ADMIN' ORDER BY a.createdAt DESC")
    Page<AuditLog> findAdminActivities(Pageable pageable);

    /**
     * 복합 조건으로 감사 로그 조회 (관리자용)
     */
    @Query("""
        SELECT a FROM AuditLog a JOIN FETCH a.actor
        WHERE (:actorId IS NULL OR a.actor.id = :actorId)
        AND (:entityType IS NULL OR a.entityType = :entityType)
        AND (:action IS NULL OR a.action = :action)
        AND (:startDate IS NULL OR a.createdAt >= :startDate)
        AND (:endDate IS NULL OR a.createdAt <= :endDate)
        ORDER BY a.createdAt DESC
        """)
    Page<AuditLog> findWithFilters(
            @Param("actorId") Long actorId,
            @Param("entityType") AuditEntityType entityType,
            @Param("action") AuditAction action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}

