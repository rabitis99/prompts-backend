package org.example.sharedprompts.dto.audit.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.audit.AuditLog;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.domain.user.User;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDto {

    private Long id;
    @JsonProperty("actor_id")
    private Long actorId;
    @JsonProperty("actor_nickname")
    private String actorNickname;
    @JsonProperty("entity_type")
    private AuditEntityType entityType;
    @JsonProperty("entity_id")
    private Long entityId;
    private AuditAction action;
    private String description;
    @JsonProperty("before_state")
    private String beforeState;
    @JsonProperty("after_state")
    private String afterState;
    @JsonProperty("ip_address")
    private String ipAddress;
    @JsonProperty("user_agent")
    private String userAgent;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static AuditLogResponseDto from(AuditLog auditLog) {
        User actor = auditLog.getActor();
        return AuditLogResponseDto.builder()
                .id(auditLog.getId())
                .actorId(actor != null ? actor.getId() : null)
                .actorNickname(actor != null ? actor.getNickname() : null)
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .action(auditLog.getAction())
                .description(auditLog.getDescription())
                .beforeState(auditLog.getBeforeState())
                .afterState(auditLog.getAfterState())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}

