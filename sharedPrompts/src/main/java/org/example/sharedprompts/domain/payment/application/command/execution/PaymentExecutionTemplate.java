package org.example.sharedprompts.domain.payment.application.command.execution;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class PaymentExecutionTemplate {

    private final PaymentProviderFactory providerFactory;
    private final PaymentJpaAdapter paymentJpaAdapter;

    public <T> Payment executeWithProvider(
            Payment payment,
            Function<PaymentProvider, T> providerAction,
            Consumer<T> resultApplier
    ) {
        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());
        T result = providerAction.apply(provider);
        resultApplier.accept(result);
        return paymentJpaAdapter.save(payment);
    }
}

