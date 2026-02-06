package org.example.sharedprompts.domain.payment.application.command.service.request;

import org.example.sharedprompts.domain.payment.application.dto.AmountProcessingResult;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.springframework.stereotype.Component;

@Component
public class PaymentCreator {

    public Payment createPayment(User user,
                                 PaymentRequestDto request,
                                 AmountProcessingResult amountResult) {
        return request.toPaymentBuilder(
                user,
                amountResult.convertedAmount(),
                amountResult.usedPointAmount()
        ).build();
    }
}

