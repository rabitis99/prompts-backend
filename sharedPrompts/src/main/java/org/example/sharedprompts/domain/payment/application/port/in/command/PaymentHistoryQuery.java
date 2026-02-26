package org.example.sharedprompts.domain.payment.application.port.in.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

/**
 * 결제 내역 조회 쿼리
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryQuery {

    private Long userId;
    private Pageable pageable;

    /**
     * 쿼리 생성
     */
    public static PaymentHistoryQuery of(Long userId, Pageable pageable) {
        return PaymentHistoryQuery.builder()
                .userId(userId)
                .pageable(pageable)
                .build();
    }
}
