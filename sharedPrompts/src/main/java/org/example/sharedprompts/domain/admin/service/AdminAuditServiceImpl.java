package org.example.sharedprompts.domain.admin.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.admin.validator.AdminValidator;
import org.example.sharedprompts.domain.audit.AuditLog;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.domain.audit.service.AuditLogService;
import org.example.sharedprompts.dto.audit.response.AuditLogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuditServiceImpl implements AdminAuditService {

    private final AuditLogService auditLogService;
    private final AdminValidator adminValidator;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getAuditLogs(
            Long actorId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        adminValidator.validateDateRange(startDate, endDate);
        adminValidator.validatePageSize(pageable, 100);

        Page<AuditLog> logs = auditLogService.getLogsWithFilters(
                actorId, entityType, action, startDate, endDate, pageable
        );
        return logs.map(AuditLogResponseDto::from);
    }
}


