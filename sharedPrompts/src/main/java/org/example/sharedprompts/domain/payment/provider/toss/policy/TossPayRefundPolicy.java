package org.example.sharedprompts.domain.payment.provider.toss.policy;

import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
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
     * 
     * @param refundedAmount 실제 환불된 금액 (필수)
     * @param originalAmount 원 결제 금액 (필수)
     * @return PaymentStatus
     * @throws IllegalArgumentException 필수 필드가 null일 때
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

