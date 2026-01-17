package org.example.sharedprompts.domain.admin.util;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.domain.audit.util.AuditLogPublisher;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.util.HttpRequestUtils;
import org.springframework.stereotype.Component;

/**
 * 관리자 활동 감사 로그를 기록하는 헬퍼 클래스
 * IP 주소와 User-Agent를 자동으로 추출하여 감사 로그를 생성합니다.
 */
@Component
@RequiredArgsConstructor
public class AdminAuditLogger {

    private final AuditLogPublisher auditLogPublisher;

    /**
     * 사용자 차단/해제 감사 로그 기록
     */
    public void logUserBlock(User admin, Long targetUserId, boolean blocked) {
        String actionDescription = blocked 
                ? String.format("사용자 차단: userId=%d", targetUserId)
                : String.format("사용자 차단 해제: userId=%d", targetUserId);
        
        auditLogPublisher.publish(
                admin,
                AuditEntityType.USER,
                targetUserId,
                blocked ? AuditAction.BLOCK : AuditAction.UNBLOCK,
                actionDescription,
                null,
                null,
                HttpRequestUtils.getClientIpAddress(),
                HttpRequestUtils.getUserAgent()
        );
    }

    /**
     * 사용자 권한 변경 감사 로그 기록
     */
    public void logUserRoleChange(User admin, Long targetUserId, String oldRole, String newRole) {
        auditLogPublisher.publish(
                admin,
                AuditEntityType.USER,
                targetUserId,
                AuditAction.ROLE_CHANGE,
                String.format("사용자 권한 변경: oldRole=%s, newRole=%s", oldRole, newRole),
                oldRole,
                newRole,
                HttpRequestUtils.getClientIpAddress(),
                HttpRequestUtils.getUserAgent()
        );
    }

    /**
     * 프롬프트 삭제 감사 로그 기록
     */
    public void logPromptDelete(User admin, Long promptId) {
        auditLogPublisher.publish(
                admin,
                AuditEntityType.PROMPT,
                promptId,
                AuditAction.DELETE,
                String.format("프롬프트 삭제: promptId=%d", promptId),
                null,
                null,
                HttpRequestUtils.getClientIpAddress(),
                HttpRequestUtils.getUserAgent()
        );
    }

    /**
     * 프롬프트 공개 상태 변경 감사 로그 기록
     */
    public void logPromptVisibilityChange(User admin, Long promptId, boolean isPublic) {
        auditLogPublisher.publish(
                admin,
                AuditEntityType.PROMPT,
                promptId,
                AuditAction.PUBLIC_TOGGLE,
                String.format("프롬프트 공개 상태 변경: promptId=%d, isPublic=%s", promptId, isPublic),
                String.valueOf(!isPublic),
                String.valueOf(isPublic),
                HttpRequestUtils.getClientIpAddress(),
                HttpRequestUtils.getUserAgent()
        );
    }

    /**
     * 신고 처리 감사 로그 기록
     */
    public void logReportProcess(User admin, Long reportId, String status, String processComment) {
        auditLogPublisher.publish(
                admin,
                AuditEntityType.REPORT,
                reportId,
                AuditAction.REPORT_PROCESS,
                String.format("신고 처리: reportId=%d, status=%s", reportId, status),
                null,
                status + (processComment != null ? ": " + processComment : ""),
                HttpRequestUtils.getClientIpAddress(),
                HttpRequestUtils.getUserAgent()
        );
    }
}

