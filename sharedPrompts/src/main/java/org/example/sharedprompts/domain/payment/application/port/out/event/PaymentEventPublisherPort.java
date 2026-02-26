package org.example.sharedprompts.domain.payment.application.port.out.event;

import org.example.sharedprompts.domain.payment.domain.event.*;

/**
 * 결제 이벤트 발행자 아웃포트
 * 도메인 이벤트를 다른 시스템에 발행하는 인터페이스
 */
public interface PaymentEventPublisherPort {

    /**
     * 결제 승인 이벤트를 발행한다
     */
    void publishPaymentApproved(PaymentApprovedEvent event);

    /**
     * 결제 확인 이벤트를 발행한다
     */
    void publishPaymentConfirmed(PaymentConfirmedEvent event);

    /**
     * 결제 취소 이벤트를 발행한다
     */
    void publishPaymentCanceled(PaymentCanceledEvent event);

    /**
     * 결제 환불 이벤트를 발행한다
     */
    void publishPaymentRefunded(PaymentRefundedEvent event);

    /**
     * 결제 실패 이벤트를 발행한다
     */
    void publishPaymentFailed(PaymentFailedEvent event);

    /**
     * 결제 만료 이벤트를 발행한다
     */
    void publishPaymentExpired(PaymentExpiredEvent event);
}
