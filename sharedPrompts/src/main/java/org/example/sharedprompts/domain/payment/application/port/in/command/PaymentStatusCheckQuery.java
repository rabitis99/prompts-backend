package org.example.sharedprompts.domain.payment.application.port.in.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 상태 조회 쿼리
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusCheckQuery {

    private Long paymentId;
    private Long userId;

    /**
     * 쿼리 생성
     */
    public static PaymentStatusCheckQuery of(Long paymentId, Long userId) {
        return PaymentStatusCheckQuery.builder()
                .paymentId(paymentId)
                .userId(userId)
                .build();
    }
}
