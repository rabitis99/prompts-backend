package org.example.sharedprompts.domain.payment.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 결제 취소 도메인 이벤트
 * 결제가 취소되었을 때 발행됨
 */
@Getter
@Builder
@AllArgsConstructor
public class PaymentCanceledEvent {

    private Long paymentId;
    private Long userId;
    private BigDecimal canceledAmount;
    private String reason;
    private LocalDateTime timestamp;

    public static PaymentCanceledEvent of(
            Long paymentId,
            Long userId,
            BigDecimal canceledAmount,
            String reason
    ) {
        return PaymentCanceledEvent.builder()
                .paymentId(paymentId)
                .userId(userId)
                .canceledAmount(canceledAmount)
                .reason(reason)
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }
}
