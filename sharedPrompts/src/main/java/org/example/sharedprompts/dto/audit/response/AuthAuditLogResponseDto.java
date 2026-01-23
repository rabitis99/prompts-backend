package org.example.sharedprompts.dto.audit.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.audit.auth.AuthAuditLog;
import org.example.sharedprompts.domain.audit.auth.enums.AuthEventType;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.user.enums.Provider;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class AuthAuditLogResponseDto {

    private Long id;
    
    @JsonProperty("event_type")
    private AuthEventType eventType;
    
    private Provider provider;
    
    @JsonProperty("provider_id_hash")
    private String providerIdHash;
    
    @JsonProperty("user_id")
    private Long userId;
    
    @JsonProperty("fail_reason")
    private AuthFailReason failReason;
    
    @JsonProperty("ip_address")
    private String ipAddress;
    
    @JsonProperty("user_agent")
    private String userAgent;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static AuthAuditLogResponseDto from(AuthAuditLog authAuditLog) {
        return AuthAuditLogResponseDto.builder()
                .id(authAuditLog.getId())
                .eventType(authAuditLog.getEventType())
                .provider(authAuditLog.getProvider())
                .providerIdHash(authAuditLog.getProviderIdHash())
                .userId(authAuditLog.getUserId())
                .failReason(authAuditLog.getFailReason())
                .ipAddress(authAuditLog.getIpAddress())
                .userAgent(authAuditLog.getUserAgent())
                .createdAt(authAuditLog.getCreatedAt())
                .build();
    }
}

