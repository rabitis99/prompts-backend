package org.example.sharedprompts.domain.payment.application.command.service.confirm;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.service.PaymentValidator;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentConfirmValidator {

    private final PaymentValidator paymentValidator;

    public void validateAndPreparePayment(Payment payment, PaymentConfirmRequest request) {
        paymentValidator.validatePaymentKey(request.getPaymentKey(), payment.getPaymentMethod());

        if (payment.getPaymentMethod() != PaymentMethod.KAKAO_PAY) {
            if (request.getPaymentKey() == null || request.getPaymentKey().isEmpty()) {
                throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                        "비카카오 결제는 paymentKey가 필수입니다");
            }
            payment.updateExternalPaymentId(request.getPaymentKey());
        }
    }
}

