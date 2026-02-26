package org.example.sharedprompts.domain.payment.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 결제 만료 도메인 이벤트
 * 미결제 상태의 결제가 만료되었을 때 발행됨
 */
@Getter
@Builder
@AllArgsConstructor
public class PaymentExpiredEvent {

    private Long paymentId;
    private Long userId;
    private LocalDateTime timestamp;

    public static PaymentExpiredEvent of(Long paymentId, Long userId) {
        return PaymentExpiredEvent.builder()
                .paymentId(paymentId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
