package org.example.sharedprompts.domain.payment.application.port.in.usecase;

import org.example.sharedprompts.domain.payment.application.port.in.command.RefundPaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentRefundResult;

/**
 * 결제 환불 유스케이스 포트
 * 사용자의 결제 환불 요청을 처리하는 비즈니스 로직
 */
public interface PaymentRefundUseCase {

    /**
     * 결제를 환불한다.
     *
     * @param command 결제 환불 명령
     * @return 결제 환불 결과
     */
    PaymentRefundResult refund(RefundPaymentCommand command);
}
