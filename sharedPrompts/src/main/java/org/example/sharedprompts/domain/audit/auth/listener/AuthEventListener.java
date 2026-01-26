package org.example.sharedprompts.domain.audit.auth.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.audit.auth.AuthAuditLog;
import org.example.sharedprompts.domain.audit.auth.event.AuthEvent;
import org.example.sharedprompts.domain.audit.auth.service.AuthAuditLogService;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 인증 보안 이벤트 리스너
 * AFTER_COMMIT 페이즈에서 처리하여 메인 트랜잭션이 완료된 후에만 로그를 저장합니다.
 * 
 * 실패해도 인증 로직에 영향을 주지 않도록 예외를 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventListener {

    private final AuthAuditLogService authAuditLogService;

    /**
     * 인증 이벤트 처리
     * - AFTER_COMMIT: 트랜잭션 커밋 후 실행
     * - @Async: 비동기 처리로 메인 트랜잭션에 영향 없음
     * 
     * <p>예외 처리:
     * - try-catch로 로깅은 유지하되, 예외를 재던져서 AsyncUncaughtExceptionHandler가 처리하도록 함
     * - AsyncUncaughtExceptionHandler가 알람 발송 및 메트릭 기록을 수행
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuthEvent(AuthEvent event) {
        String maskedUserId = SensitiveDataMasker.mask(String.valueOf(event.getUserId()));
        try {
            AuthAuditLog auditLog = AuthAuditLog.builder()
                    .eventType(event.getEventType())
                    .provider(event.getProvider())
                    .providerIdHash(event.getProviderIdHash())
                    .userId(event.getUserId())
                    .failReason(event.getFailReason())
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .build();

            authAuditLogService.saveLog(auditLog);
            
            log.debug("인증 이벤트 로그 저장 완료: eventType={}, provider={}, userId={}",
                    event.getEventType(), event.getProvider(), maskedUserId);
        } catch (Exception e) {
            //  실패 시 로깅 후 예외를 재던져 AsyncUncaughtExceptionHandler가 알람 및 메트릭을 처리합니다.
            log.error("인증 이벤트 로그 처리 실패: eventType={}, provider={}, userId={}",
                    event.getEventType(), event.getProvider(), maskedUserId, e);
            // AsyncUncaughtExceptionHandler가 알람 발송 및 메트릭 기록을 수행하도록 예외 재던지기
            throw new RuntimeException("Failed to save auth audit log: eventType=" +
                    event.getEventType() + ", provider=" + event.getProvider(), e);
        }
    }
}

