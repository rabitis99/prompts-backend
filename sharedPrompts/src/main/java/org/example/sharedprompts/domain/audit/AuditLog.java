package org.example.sharedprompts.domain.audit;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.entity.BaseEntity;

/**
 * 시스템 전반의 중요한 활동을 기록하는 감사 로그 엔티티
 * 관리자 작업, 사용자 활동, 데이터 변경 등을 추적합니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_actor", columnList = "actor_id"),
        @Index(name = "idx_audit_entity", columnList = "entity_type,entity_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_created_at", columnList = "created_at")
})
public class AuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 작업을 수행한 사용자 (관리자 또는 일반 사용자)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    /**
     * 작업이 수행된 엔티티 타입 (USER, PROMPT, COMMENT, REPORT 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private AuditEntityType entityType;

    /**
     * 작업이 수행된 엔티티의 ID
     */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * 수행된 작업 타입 (CREATE, UPDATE, DELETE, BLOCK, UNBLOCK 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    /**
     * 작업 상세 설명
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 변경 전 상태 (JSON 형식)
     */
    @Column(name = "before_state", columnDefinition = "TEXT")
    private String beforeState;

    /**
     * 변경 후 상태 (JSON 형식)
     */
    @Column(name = "after_state", columnDefinition = "TEXT")
    private String afterState;

    /**
     * IP 주소
     */
    @Column(length = 50)
    private String ipAddress;

    /**
     * User Agent 정보
     * 일반적인 User-Agent는 100-300자이지만, 일부 특수한 경우(긴 브라우저 확장 목록 등)를 고려하여 1000자로 설정
     */
    @Column(name = "user_agent", length = 1000)
    private String userAgent;
}

