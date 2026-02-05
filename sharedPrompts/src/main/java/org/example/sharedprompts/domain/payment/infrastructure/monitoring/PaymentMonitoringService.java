package org.example.sharedprompts.domain.payment.infrastructure.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.messaging.notification.PaymentNotificationService;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.event.PaymentEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

/**
 * 결제 모니터링 서비스
 * 결제 실패 모니터링 및 알림 기능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMonitoringService {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentNotificationService notificationService;
    private final PaymentMetrics paymentMetrics;

    /**
     * 결제 실패 이벤트 리스너
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handlePaymentFailed(PaymentEvent.PaymentFailed event) {
        log.warn("결제 실패 감지: paymentId={}, userId={}, reason={}, paymentMethod={}",
                event.paymentId(), event.userId(), event.reason(), event.paymentMethod());

        // 알림 발송
        notificationService.sendPaymentFailureNotification(
                event.paymentId(),
                event.userId(),
                event.reason(),
                event.paymentMethod()
        );

        // 모니터링 메트릭 업데이트
        paymentMetrics.recordPaymentFailure(event.paymentMethod(), event.reason(), 0);
    }

    /**
     * 결제 성공 이벤트 리스너
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handlePaymentSucceeded(PaymentEvent.PaymentSucceeded event) {
        log.info("결제 성공: paymentId={}, userId={}, paymentMethod={}",
                event.paymentId(), event.userId(), event.paymentMethod());

        // 알림 발송
        Optional<Payment> paymentOpt = paymentJpaAdapter.findById(event.paymentId());
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            notificationService.sendPaymentSuccessNotification(
                    event.paymentId(),
                    event.userId(),
                    event.paymentMethod(),
                    payment.getAmount().toString()
            );
        }

        // 성공 메트릭 업데이트
        paymentMetrics.recordPaymentSuccess(event.paymentMethod(), 0);
    }
}

