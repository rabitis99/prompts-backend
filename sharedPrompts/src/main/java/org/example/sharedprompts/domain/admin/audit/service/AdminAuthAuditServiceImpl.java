package org.example.sharedprompts.domain.admin.audit.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.admin.validator.AdminValidator;
import org.example.sharedprompts.domain.audit.auth.AuthAuditLog;
import org.example.sharedprompts.domain.audit.auth.enums.AuthEventType;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.audit.auth.service.AuthAuditLogService;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.dto.audit.response.AuthAuditLogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthAuditServiceImpl implements AdminAuthAuditService {

    private final AuthAuditLogService authAuditLogService;
    private final AdminValidator adminValidator;

    @Override
    @Transactional(readOnly = true)
    public Page<AuthAuditLogResponseDto> getAuthAuditLogs(
            AuthEventType eventType,
            Provider provider,
            Long userId,
            AuthFailReason failReason,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        adminValidator.validateDateRange(startDate, endDate);
        adminValidator.validatePageSize(pageable, 100);

        Page<AuthAuditLog> logs = authAuditLogService.getLogsWithFilters(
                eventType, provider, userId, failReason, startDate, endDate, pageable
        );
        return logs.map(AuthAuditLogResponseDto::from);
    }
}

