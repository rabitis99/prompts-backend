package org.example.sharedprompts.domain.payment.application.port.in.usecase;

import org.example.sharedprompts.domain.payment.application.port.in.command.PaymentWebhookCommand;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;

import java.util.Optional;

/**
 * 결제 웹훅 처리 유스케이스 포트
 * 외부 결제 제공자의 웹훅을 처리하는 비즈니스 로직
 */
public interface PaymentWebhookHandlingUseCase {

    /**
     * 웹훅을 처리한다.
     *
     * @param command 결제 웹훅 명령
     * @return 처리된 결제 (없으면 empty)
     */
    Optional<Payment> handleWebhook(PaymentWebhookCommand command);
}
