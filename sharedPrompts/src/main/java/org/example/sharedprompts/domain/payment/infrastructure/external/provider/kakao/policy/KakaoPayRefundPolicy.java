package org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.policy;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class KakaoPayRefundPolicy {

    /**
     * KakaoPay 환불 상태 결정
     */
    public PaymentStatus determineStatus(
            BigDecimal refundedAmount,
            BigDecimal originalAmount
    ) {
        if (refundedAmount == null) {
            throw new IllegalArgumentException("refundedAmount는 null일 수 없습니다");
        }
        if (originalAmount != null && refundedAmount.compareTo(originalAmount) >= 0) {
            return PaymentStatus.REFUNDED;
        }
        return PaymentStatus.PARTIALLY_REFUNDED;
    }
}
