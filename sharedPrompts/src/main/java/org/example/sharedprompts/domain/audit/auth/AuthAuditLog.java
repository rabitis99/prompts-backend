package org.example.sharedprompts.domain.audit.auth;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.audit.auth.enums.AuthEventType;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.global.entity.BaseEntity;

/**
 * 인증 보안 이벤트 로그 엔티티
 * 관리자가 인증 이력을 조회할 수 있도록 저장합니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "auth_audit_logs", indexes = {
        @Index(name = "idx_auth_event_type", columnList = "event_type"),
        @Index(name = "idx_auth_provider", columnList = "provider"),
        @Index(name = "idx_auth_user_id", columnList = "user_id"),
        @Index(name = "idx_auth_fail_reason", columnList = "fail_reason"),
        @Index(name = "idx_auth_created_at", columnList = "created_at"),
        @Index(name = "idx_auth_provider_hash", columnList = "provider_id_hash")
})
public class AuthAuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이벤트 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuthEventType eventType;

    /**
     * 인증 제공자
     * null인 경우: OAuth 코드 검증 실패, 토큰 갱신 실패(사용자 정보 없음) 등 provider를 알 수 없는 경우
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = true, length = 20)
    private Provider provider;

    /**
     * providerId 또는 email의 해시값
     * 원문은 저장하지 않고 해시값만 저장합니다.
     */
    @Column(name = "provider_id_hash", length = 64)
    private String providerIdHash;

    /**
     * 사용자 ID (성공한 경우에만 존재)
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 실패 사유 (실패한 경우에만 존재)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "fail_reason", length = 50)
    private AuthFailReason failReason;

    /**
     * IP 주소
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * User-Agent 정보
     */
    @Column(name = "user_agent", length = 1000)
    private String userAgent;
}

