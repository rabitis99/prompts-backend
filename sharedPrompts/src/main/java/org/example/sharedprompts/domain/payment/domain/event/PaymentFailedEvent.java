package org.example.sharedprompts.domain.payment.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 결제 실패 도메인 이벤트
 * 결제가 실패했을 때 발행됨
 */
@Getter
@Builder
@AllArgsConstructor
public class PaymentFailedEvent {

    private Long paymentId;
    private Long userId;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime timestamp;

    public static PaymentFailedEvent of(
            Long paymentId,
            Long userId,
            String errorCode,
            String errorMessage
    ) {
        return PaymentFailedEvent.builder()
                .paymentId(paymentId)
                .userId(userId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
