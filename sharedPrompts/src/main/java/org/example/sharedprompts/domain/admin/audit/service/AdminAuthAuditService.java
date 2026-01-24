package org.example.sharedprompts.domain.admin.audit.service;

import org.example.sharedprompts.domain.audit.auth.enums.AuthEventType;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.dto.audit.response.AuthAuditLogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AdminAuthAuditService {

    Page<AuthAuditLogResponseDto> getAuthAuditLogs(
            AuthEventType eventType,
            Provider provider,
            Long userId,
            AuthFailReason failReason,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
}

