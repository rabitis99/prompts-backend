package org.example.sharedprompts.domain.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PayPal 설정 Properties (Immutable)
 */
@Getter
@Component
public class PaypalProperties {

    private final String clientId;
    private final String clientSecret;
    private final String webhookId;

    public PaypalProperties(
            @Value("${payment.paypal.client-id:}") String clientId,
            @Value("${payment.paypal.client-secret:}") String clientSecret,
            @Value("${payment.paypal.webhook-id:}") String webhookId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.webhookId = webhookId;
    }
}

