package org.example.sharedprompts.domain.payment.application.port.in.usecase;

import org.example.sharedprompts.domain.payment.application.port.in.command.*;
import org.example.sharedprompts.domain.payment.application.port.in.result.*;

public interface PaymentCommandUseCase {

    PaymentApprovalResult approve(ApprovePaymentCommand command);

    PaymentCancellationResult cancel(CancelPaymentCommand command);

    PaymentRefundResult refund(RefundPaymentCommand command);

    PaymentConfirmationResult confirm(ConfirmPaymentCommand command);
}
