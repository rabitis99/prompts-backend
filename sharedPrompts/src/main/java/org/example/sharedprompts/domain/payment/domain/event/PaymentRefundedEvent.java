package org.example.sharedprompts.domain.payment.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 결제 환불 도메인 이벤트
 * 결제가 환불되었을 때 발행됨
 */
@Getter
@Builder
@AllArgsConstructor
public class PaymentRefundedEvent {

    private Long paymentId;
    private Long userId;
    private BigDecimal refundAmount;
    private String reason;
    private LocalDateTime timestamp;

    public static PaymentRefundedEvent of(
            Long paymentId,
            Long userId,
            BigDecimal refundAmount,
            String reason
    ) {
        return PaymentRefundedEvent.builder()
                .paymentId(paymentId)
                .userId(userId)
                .refundAmount(refundAmount)
                .reason(reason)
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }
}
