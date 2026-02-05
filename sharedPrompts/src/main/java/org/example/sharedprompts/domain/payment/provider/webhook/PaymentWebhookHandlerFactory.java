package org.example.sharedprompts.domain.payment.provider.webhook;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
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
 * PaymentWebhookHandler 팩토리
 */
@Component
@RequiredArgsConstructor
public class PaymentWebhookHandlerFactory {
    
    private final List<PaymentWebhookHandler> handlers;
    private Map<PaymentMethod, PaymentWebhookHandler> handlerMap;
    
    @PostConstruct
    public void init() {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(
                        PaymentWebhookHandler::getPaymentMethod,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException(
                                    "Duplicate PaymentWebhookHandler for " + a.getPaymentMethod());
                        },
                        () -> new EnumMap<>(PaymentMethod.class)
                ));
    }
    
    /**
     * 결제 수단에 맞는 WebhookHandler 조회
     */
    public PaymentWebhookHandler getHandler(PaymentMethod paymentMethod) {
        return Optional.ofNullable(handlerMap.get(paymentMethod))
                .orElseThrow(() ->
                        new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR,
                                "지원하지 않는 결제 수단의 WebhookHandler입니다: " + paymentMethod));
    }
}

