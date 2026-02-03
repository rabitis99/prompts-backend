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
    private final String returnUrl;
    private final String cancelUrl;

    public PaypalProperties(
            @Value("${payment.paypal.client-id:}") String clientId,
            @Value("${payment.paypal.client-secret:}") String clientSecret,
            @Value("${payment.paypal.webhook-id:}") String webhookId,
            @Value("${payment.paypal.return-url:}") String returnUrl,
            @Value("${payment.paypal.cancel-url:}") String cancelUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.webhookId = webhookId;
        this.returnUrl = returnUrl;
        this.cancelUrl = cancelUrl;
    }
}




