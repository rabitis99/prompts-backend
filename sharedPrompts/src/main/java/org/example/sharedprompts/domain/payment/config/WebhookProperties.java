package org.example.sharedprompts.domain.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Webhook 설정 Properties (Immutable)
 */
@Getter
@Component
public class WebhookProperties {

    private final String secret;

    public WebhookProperties(
            @Value("${payment.webhook.secret:}") String secret) {
        this.secret = secret;
    }
}










