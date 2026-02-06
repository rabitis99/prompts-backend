package org.example.sharedprompts.domain.payment.application.command.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentPreparationResultHandler {

    public PreparePaymentResult handlePreparationResult(Payment payment, PaymentProvider.PrepareResult prepareResult) {
        if (prepareResult.required() && prepareResult.redirectUrl() != null) {
            payment.updateExternalPaymentId(prepareResult.tid());
            log.info("결제 준비 완료: paymentId={}, paymentMethod={}, tid={}, redirectUrl={}",
                    payment.getId(), payment.getPaymentMethod(),
                    prepareResult.tid(), prepareResult.redirectUrl());
            return PreparePaymentResult.success(prepareResult.tid(), prepareResult.redirectUrl());
        }

        log.warn("결제 준비 결과가 유효하지 않음: paymentId={}, paymentMethod={}, required={}, redirectUrl={}",
                payment.getId(), payment.getPaymentMethod(),
                prepareResult.required(), prepareResult.redirectUrl());
        return PreparePaymentResult.notRequired();
    }
}

