package org.example.sharedprompts.domain.payment.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.application.port.in.command.PaymentHistoryQuery;
import org.example.sharedprompts.domain.payment.application.port.in.command.PaymentStatusCheckQuery;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentHistoryResult;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentStatusResult;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.PaymentHistoryQueryUseCase;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.PaymentQueryUseCase;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.PaymentStatusCheckUseCase;
import org.springframework.data.domain.Page;

@RequiredArgsConstructor
public class PaymentQueryUseCaseImpl implements PaymentQueryUseCase {

    private final PaymentStatusCheckUseCase statusCheckUseCase;
    private final PaymentHistoryQueryUseCase historyQueryUseCase;

    @Override
    public PaymentStatusResult checkStatus(PaymentStatusCheckQuery query) {
        return statusCheckUseCase.checkStatus(query);
    }

    @Override
    public Page<PaymentHistoryResult> getHistory(PaymentHistoryQuery query) {
        return historyQueryUseCase.getHistory(query);
    }
}
