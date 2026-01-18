package org.example.sharedprompts.domain.audit.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;

/**
 * 감사 로그 생성을 위한 이벤트
 * 
 * JPA Entity를 직접 포함하지 않고 스냅샷 값만 포함하여
 * AFTER_COMMIT 단계에서 안전하게 사용할 수 있도록 합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    /**
     * 액터 사용자 ID (스냅샷)
     */
    private Long actorId;

    /**
     * 액터 식별자 (email 또는 nickname, 스냅샷)
     */
    private String actorIdentifier;

    private AuditEntityType entityType;
    private Long entityId;
    private AuditAction action;
    private String description;
    private String beforeState;
    private String afterState;
    private String ipAddress;
    private String userAgent;
}

