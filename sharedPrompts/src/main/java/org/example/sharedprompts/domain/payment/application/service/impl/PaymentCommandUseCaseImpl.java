package org.example.sharedprompts.domain.payment.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.application.port.in.command.*;
import org.example.sharedprompts.domain.payment.application.port.in.result.*;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.*;

@RequiredArgsConstructor
public class PaymentCommandUseCaseImpl implements PaymentCommandUseCase {

    private final PaymentApprovalUseCase approvalUseCase;
    private final PaymentCancellationUseCase cancellationUseCase;
    private final PaymentRefundUseCase refundUseCase;
    private final PaymentConfirmationUseCase confirmationUseCase;

    @Override
    public PaymentApprovalResult approve(ApprovePaymentCommand command) {
        return approvalUseCase.approve(command);
    }

    @Override
    public PaymentCancellationResult cancel(CancelPaymentCommand command) {
        return cancellationUseCase.cancel(command);
    }

    @Override
    public PaymentRefundResult refund(RefundPaymentCommand command) {
        return refundUseCase.refund(command);
    }

    @Override
    public PaymentConfirmationResult confirm(ConfirmPaymentCommand command) {
        return confirmationUseCase.confirm(command);
    }
}
