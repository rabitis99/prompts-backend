package org.example.sharedprompts.domain.payment.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 확인 도메인 이벤트
 * 결제가 제공자에서 확인되었을 때 발행됨
 */
@Getter
@Builder
@AllArgsConstructor
public class PaymentConfirmedEvent {

    private Long paymentId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private String externalPaymentId;
    private LocalDateTime timestamp;

    public static PaymentConfirmedEvent of(
            Long paymentId,
            Long userId,
            BigDecimal amount,
            String currency,
            String externalPaymentId
    ) {
        return PaymentConfirmedEvent.builder()
                .paymentId(paymentId)
                .userId(userId)
                .amount(amount)
                .currency(currency)
                .externalPaymentId(externalPaymentId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
