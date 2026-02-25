package org.example.sharedprompts.domain.payment.application.port.in.usecase;

import org.example.sharedprompts.domain.payment.application.port.in.command.ConfirmPaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentConfirmationResult;

/**
 * 결제 확인 유스케이스 포트
 * 결제 제공자(PG)로부터 승인 완료 후 확인하는 비즈니스 로직
 */
public interface PaymentConfirmationUseCase {

    /**
     * 결제를 확인한다.
     *
     * @param command 결제 확인 명령
     * @return 결제 확인 결과
     */
    PaymentConfirmationResult confirm(ConfirmPaymentCommand command);
}
