package org.example.sharedprompts.domain.audit.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.domain.user.User;

/**
 * 감사 로그 생성을 위한 이벤트
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    private User actor;
    private AuditEntityType entityType;
    private Long entityId;
    private AuditAction action;
    private String description;
    private String beforeState;
    private String afterState;
    private String ipAddress;
    private String userAgent;
}

