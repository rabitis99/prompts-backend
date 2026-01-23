package org.example.sharedprompts.domain.audit.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.audit.auth.AuthAuditLog;
import org.example.sharedprompts.domain.audit.auth.enums.AuthEventType;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.audit.auth.repository.AuthAuditLogRepository;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인증 보안 이벤트 로그 서비스 구현
 */
@Service
@RequiredArgsConstructor
public class AuthAuditLogServiceImpl implements AuthAuditLogService {

    private final AuthAuditLogRepository authAuditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(AuthAuditLog auditLog) {
        authAuditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuthAuditLog> getLogsWithFilters(
            AuthEventType eventType,
            Provider provider,
            Long userId,
            AuthFailReason failReason,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        return authAuditLogRepository.findWithFilters(
                eventType, provider, userId, failReason, startDate, endDate, pageable
        );
    }
}

