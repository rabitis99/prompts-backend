package org.example.sharedprompts.domain.notification.event;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.notification.metrics.NotificationMetrics;
import org.example.sharedprompts.domain.notification.service.NotificationSseService;
import org.example.sharedprompts.dto.notification.response.NotificationResponseDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 저장 이벤트 리스너
 * - 트랜잭션 커밋 후 SSE 전송 처리
 * - 알림 저장과 SSE 전송을 분리하여 트랜잭션 범위 최적화
 */
@Component
@RequiredArgsConstructor
public class NotificationSseEventListener {

    private final NotificationSseService sseService;
    private final NotificationMetrics metrics;

    /**
     * 알림 저장 완료 후 SSE 전송
     * - AFTER_COMMIT: 트랜잭션 커밋 후 실행
     * - SSE 전송 실패해도 알림은 이미 저장되었으므로 예외를 던지지 않음
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationSaved(NotificationSavedEvent event) {
        try {
            NotificationResponseDto dto = NotificationResponseDto.from(event.notification());
            sseService.sendNotification(event.userId(), dto);
            metrics.recordSseSent(event.notification().getType().name());
        } catch (Exception e) {
            metrics.recordSseFailed(event.notification().getType().name());
        }
    }
}

