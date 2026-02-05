package org.example.sharedprompts.domain.payment.infrastructure.external.provider;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PaymentProvider 팩토리
 */
@Component
@RequiredArgsConstructor
public class PaymentProviderFactory {
    
    private final List<PaymentProvider> providers;
    private Map<PaymentMethod, PaymentProvider> providerMap;
    
    @PostConstruct
    public void init() {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(
                        PaymentProvider::getPaymentMethod,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException(
                                    "Duplicate PaymentProvider for " + a.getPaymentMethod());
                        },
                        () -> new EnumMap<>(PaymentMethod.class)
                ));
    }
    
    /**
     * 결제 수단에 맞는 Provider 조회
     */
    public PaymentProvider getProvider(PaymentMethod paymentMethod) {
        return Optional.ofNullable(providerMap.get(paymentMethod))
                .orElseThrow(() ->
                        new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR,
                                "지원하지 않는 결제 수단입니다: " + paymentMethod));
    }
}

