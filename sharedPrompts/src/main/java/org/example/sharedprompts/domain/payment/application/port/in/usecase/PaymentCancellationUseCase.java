package org.example.sharedprompts.domain.payment.application.port.in.usecase;

import org.example.sharedprompts.domain.payment.application.port.in.command.CancelPaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentCancellationResult;

/**
 * 결제 취소 유스케이스 포트
 * 사용자의 결제 취소 요청을 처리하는 비즈니스 로직
 */
public interface PaymentCancellationUseCase {

    /**
     * 결제를 취소한다.
     *
     * @param command 결제 취소 명령
     * @return 결제 취소 결과
     */
    PaymentCancellationResult cancel(CancelPaymentCommand command);
}
