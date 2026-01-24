package org.example.sharedprompts.domain.audit.auth.service;

import org.example.sharedprompts.domain.audit.auth.AuthAuditLog;
import org.example.sharedprompts.domain.audit.auth.enums.AuthEventType;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * 인증 보안 이벤트 로그 서비스
 */
public interface AuthAuditLogService {

    /**
     * 인증 이벤트 로그 저장
     */
    void saveLog(AuthAuditLog auditLog);

    /**
     * 복합 필터링으로 인증 이벤트 로그 조회
     */
    Page<AuthAuditLog> getLogsWithFilters(
            AuthEventType eventType,
            Provider provider,
            Long userId,
            AuthFailReason failReason,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
}

