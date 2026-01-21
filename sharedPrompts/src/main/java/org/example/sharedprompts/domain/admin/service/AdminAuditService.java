package org.example.sharedprompts.domain.admin.service;

import java.time.LocalDateTime;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.dto.audit.response.AuditLogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminAuditService {

    Page<AuditLogResponseDto> getAuditLogs(
            Long actorId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
}


