package org.example.sharedprompts.domain.payment.infrastructure.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.messaging.event.PaymentEvent;
import org.example.sharedprompts.domain.payment.infrastructure.messaging.notification.PaymentNotificationService;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 결제 모니터링 서비스
 * 결제 실패/성공 모니터링 및 알림 기능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMonitoringService {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentNotificationService notificationService;
    private final PaymentMetrics paymentMetrics;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handlePaymentFailed(PaymentEvent.PaymentFailed event) {
        try {
            log.warn("결제 실패 감지: paymentId={}, userId={}, reason={}, paymentMethod={}",
                    event.paymentId(), event.userId(), event.reason(), event.paymentMethod());

            notificationService.sendPaymentFailureNotification(
                    event.paymentId(),
                    event.userId(),
                    event.reason(),
                    event.paymentMethod()
            );

            long durationMillis = paymentJpaAdapter.findById(event.paymentId())
                    .filter(p -> p.getCreatedAt() != null)
                    .map(p -> Math.max(0L, Duration.between(p.getCreatedAt(), LocalDateTime.now()).toMillis()))
                    .orElse(0L);

            paymentMetrics.recordPaymentFailure(event.paymentMethod(), event.reason(), durationMillis);
        } catch (Exception e) {
            log.error("결제 실패 이벤트 처리 실패: paymentId={}, userId={}", event.paymentId(), event.userId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handlePaymentSucceeded(PaymentEvent.PaymentSucceeded event) {
        try {
            log.info("결제 성공: paymentId={}, userId={}, paymentMethod={}",
                    event.paymentId(), event.userId(), event.paymentMethod());

            Optional<Payment> paymentOpt = paymentJpaAdapter.findById(event.paymentId());
            
            String amount = paymentOpt
                    .map(Payment::getAmount)
                    .map(Object::toString)
                    .orElse("0");
            notificationService.sendPaymentSuccessNotification(
                    event.paymentId(),
                    event.userId(),
                    event.paymentMethod(),
                    amount
            );

            long durationMillis = paymentOpt
                    .filter(p -> p.getCreatedAt() != null && p.getApprovedAt() != null)
                    .map(p -> Math.max(0L, Duration.between(p.getCreatedAt(), p.getApprovedAt()).toMillis()))
                    .orElse(0L);

            paymentMetrics.recordPaymentSuccess(event.paymentMethod(), durationMillis);
        } catch (Exception e) {
            log.error("결제 성공 이벤트 처리 실패: paymentId={}, userId={}", event.paymentId(), event.userId(), e);
        }
    }
}
