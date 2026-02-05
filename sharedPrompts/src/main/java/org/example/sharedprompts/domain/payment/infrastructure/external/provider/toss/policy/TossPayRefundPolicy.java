package org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.policy;

import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * TossPay 환불 정책
 * 
 * <p>단일 책임: TossPay 환불 상태 결정만 담당
 */
@Component
public class TossPayRefundPolicy {

    /**
     * TossPay 환불 상태 결정
     */
    public PaymentStatus determineStatus(BigDecimal refundedAmount, BigDecimal originalAmount) {
        if (refundedAmount == null) {
            throw new IllegalArgumentException("refundedAmount는 필수입니다");
        }
        if (originalAmount == null) {
            throw new IllegalArgumentException("originalAmount는 필수입니다");
        }

        if (refundedAmount.compareTo(originalAmount) >= 0) {
            return PaymentStatus.REFUNDED;
        }
        return PaymentStatus.PARTIALLY_REFUNDED;
    }
}

