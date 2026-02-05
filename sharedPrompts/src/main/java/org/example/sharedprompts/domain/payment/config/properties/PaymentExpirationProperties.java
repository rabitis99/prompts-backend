package org.example.sharedprompts.domain.payment.config.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 결제 만료 설정 Properties (Immutable)
 */
@Getter
@Component
public class PaymentExpirationProperties {

    private final int expirationMinutes;

    public PaymentExpirationProperties(
            @Value("${payment.expiration.minutes:30}") int expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }
}



