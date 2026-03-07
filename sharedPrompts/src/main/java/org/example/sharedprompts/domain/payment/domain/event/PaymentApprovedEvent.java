package org.example.sharedprompts.domain.payment.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 결제 승인 도메인 이벤트
 * 결제가 승인되었을 때 발행됨
 */
@Getter
@Builder
@AllArgsConstructor
public class PaymentApprovedEvent {

    private Long paymentId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod method;
    private String externalPaymentId;
    private LocalDateTime timestamp;
    private String metadata; // 추가 메타데이터 (JSON)

    public static PaymentApprovedEvent of(
            Long paymentId,
            Long userId,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            String externalPaymentId
    ) {
        return PaymentApprovedEvent.builder()
                .paymentId(paymentId)
                .userId(userId)
                .amount(amount)
                .currency(currency)
                .method(method)
                .externalPaymentId(externalPaymentId)
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }
}
