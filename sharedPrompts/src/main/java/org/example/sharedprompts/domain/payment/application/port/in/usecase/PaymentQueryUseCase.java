package org.example.sharedprompts.domain.payment.application.port.in.usecase;

import org.example.sharedprompts.domain.payment.application.port.in.command.PaymentHistoryQuery;
import org.example.sharedprompts.domain.payment.application.port.in.command.PaymentStatusCheckQuery;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentHistoryResult;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentStatusResult;
import org.springframework.data.domain.Page;

public interface PaymentQueryUseCase {

    PaymentStatusResult checkStatus(PaymentStatusCheckQuery query);

    Page<PaymentHistoryResult> getHistory(PaymentHistoryQuery query);
}
