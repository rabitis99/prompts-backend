package org.example.sharedprompts.domain.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 결제 만료 설정 Properties (Immutable)
 */
@Getter
@Component
public class PaymentExpirationProperties {

    /**
     * PENDING 상태 결제의 만료 시간 (분 단위)
     * 기본값: 30분
     */
    private final int expirationMinutes;

    public PaymentExpirationProperties(
            @Value("${payment.expiration.minutes:30}") int expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }
}



