package org.example.sharedprompts.domain.payment.application.port.in.usecase;

import org.example.sharedprompts.domain.payment.application.port.in.command.ApprovePaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentApprovalResult;

/**
 * 결제 승인 유스케이스 포트
 * 사용자의 결제 요청을 처리하는 비즈니스 로직
 */
public interface PaymentApprovalUseCase {

    /**
     * 결제를 승인한다.
     *
     * @param command 결제 승인 명령
     * @return 결제 승인 결과
     */
    PaymentApprovalResult approve(ApprovePaymentCommand command);
}
