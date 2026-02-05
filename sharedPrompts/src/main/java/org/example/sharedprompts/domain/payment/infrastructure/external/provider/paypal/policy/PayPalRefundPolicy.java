package org.example.sharedprompts.domain.payment.infrastructure.external.provider.paypal.policy;

import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * PayPal 환불 정책
 * 
 * <p>단일 책임: PayPal 환불 상태 결정만 담당
 */
@Component
public class PayPalRefundPolicy {

    /**
     * PayPal 환불 상태 결정
     * 
     * @param refundedAmount 실제 환불된 금액 (필수)
     * @param originalAmount 원 결제 금액 (필수)
     * @param paypalStatus PayPal 응답 상태 (선택)
     * @return PaymentStatus
     * @throws IllegalArgumentException 필수 필드가 null일 때
     */
    public PaymentStatus determineStatus(BigDecimal refundedAmount, BigDecimal originalAmount, String paypalStatus) {
        if (refundedAmount == null) {
            throw new IllegalArgumentException("refundedAmount는 필수입니다");
        }
        if (originalAmount == null) {
            throw new IllegalArgumentException("originalAmount는 필수입니다");
        }

        // PayPal 응답의 status 필드 우선 사용
        if (paypalStatus != null && !paypalStatus.isEmpty()) {
            return switch (paypalStatus) {
                case "COMPLETED" -> PaymentStatus.REFUNDED;
                case "PARTIALLY_REFUNDED" -> PaymentStatus.PARTIALLY_REFUNDED;
                default -> PaymentStatus.PARTIALLY_REFUNDED;
            };
        }

        // status가 없으면 금액 비교
        if (refundedAmount.compareTo(originalAmount) >= 0) {
            return PaymentStatus.REFUNDED;
        }
        return PaymentStatus.PARTIALLY_REFUNDED;
    }
}

