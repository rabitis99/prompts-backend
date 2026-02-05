package org.example.sharedprompts.domain.payment.application.command.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.infrastructure.idempotency.IdempotencyService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentPreparationService {

    private final PaymentProviderFactory providerFactory;
    private final IdempotencyService idempotencyService;

    public PaymentProvider.PrepareResult preparePayment(Payment payment, BigDecimal actualAmount,
                                                       String productName, Long userId) {
        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());

        if (!provider.requiresPreparation()) {
            log.debug("결제 준비 불필요: paymentId={}, paymentMethod={}",
                    payment.getId(), payment.getPaymentMethod());
            return PaymentProvider.PrepareResult.notRequired();
        }

        String idempotencyKey = idempotencyService.generateForPayment(payment);
        payment.updateIdempotencyKey(idempotencyKey);

        String safeProductName = productName != null && !productName.isBlank()
                ? productName : "상품";

        return provider.preparePayment(
                Long.toString(payment.getId()),
                actualAmount,
                payment.getCurrency(),
                safeProductName,
                Long.toString(userId),
                idempotencyKey
        );
    }
}

