package org.example.sharedprompts.domain.payment.domain.policy;

import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentValidationException;

/**
 * 결제 상태 전이 정책
 * 결제 상태 변경 규칙을 검증하는 도메인 정책
 */
public class PaymentStatusTransitionPolicy {

    /**
     * 결제를 승인할 수 있는지 검증
     */
    public static void validateCanApprove(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentValidationException(
                    "결제 상태가 PENDING이 아니므로 승인할 수 없습니다. 현재 상태: " + payment.getStatus()
            );
        }
    }

    /**
     * 결제를 취소할 수 있는지 검증
     */
    public static void validateCanCancel(Payment payment) {
        PaymentStatus status = payment.getStatus();
        if (status != PaymentStatus.PENDING && status != PaymentStatus.SUCCESS) {
            throw new PaymentValidationException(
                    "결제를 취소할 수 없는 상태입니다. 현재 상태: " + status
            );
        }
    }

    /**
     * 결제를 환불할 수 있는지 검증
     */
    public static void validateCanRefund(Payment payment) {
        PaymentStatus status = payment.getStatus();
        if (status != PaymentStatus.SUCCESS && status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new PaymentValidationException(
                    "결제를 환불할 수 없는 상태입니다. 현재 상태: " + status
            );
        }
    }

    /**
     * 환불 가능 금액을 계산
     */
    public static java.math.BigDecimal calculateRefundableAmount(Payment payment) {
        return payment.getAmount()
                .subtract(payment.getRefundedAmount() != null ? payment.getRefundedAmount() : java.math.BigDecimal.ZERO);
    }

    /**
     * 환불 가능 여부 확인
     */
    public static boolean isRefundable(Payment payment) {
        try {
            validateCanRefund(payment);
            return calculateRefundableAmount(payment).compareTo(java.math.BigDecimal.ZERO) > 0;
        } catch (PaymentValidationException e) {
            return false;
        }
    }
}
