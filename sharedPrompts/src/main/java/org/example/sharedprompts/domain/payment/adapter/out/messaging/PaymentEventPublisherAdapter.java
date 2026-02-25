package org.example.sharedprompts.domain.payment.adapter.out.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.port.out.event.PaymentEventPublisherPort;
import org.example.sharedprompts.domain.payment.domain.event.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 결제 이벤트 발행자 어댑터
 * PaymentEventPublisherPort를 구현하여 도메인 이벤트를 발행합니다.
 *
 * Spring의 ApplicationEventPublisher를 사용하여 이벤트를 비동기로 발행합니다.
 * 각 이벤트는 @EventListener 또는 @TransactionalEventListener로 수신할 수 있습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisherAdapter implements PaymentEventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    private void publishAfterCommit(Object event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    applicationEventPublisher.publishEvent(event);
                }
            });
            return;
        }
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishPaymentApproved(PaymentApprovedEvent event) {
        log.info("결제 승인 이벤트 발행: paymentId={}, userId={}", event.getPaymentId(), event.getUserId());
        publishAfterCommit(event);
    }

    @Override
    public void publishPaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("결제 확인 이벤트 발행: paymentId={}, userId={}", event.getPaymentId(), event.getUserId());
        publishAfterCommit(event);
    }

    @Override
    public void publishPaymentCanceled(PaymentCanceledEvent event) {
        log.info("결제 취소 이벤트 발행: paymentId={}, userId={}", event.getPaymentId(), event.getUserId());
        publishAfterCommit(event);
    }

    @Override
    public void publishPaymentRefunded(PaymentRefundedEvent event) {
        log.info("결제 환불 이벤트 발행: paymentId={}, userId={}", event.getPaymentId(), event.getUserId());
        publishAfterCommit(event);
    }

    @Override
    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.info("결제 실패 이벤트 발행: paymentId={}, userId={}, errorCode={}",
                event.getPaymentId(), event.getUserId(), event.getErrorCode());
        publishAfterCommit(event);
    }

    @Override
    public void publishPaymentExpired(PaymentExpiredEvent event) {
        log.info("결제 만료 이벤트 발행: paymentId={}, userId={}", event.getPaymentId(), event.getUserId());
        publishAfterCommit(event);
    }
}
