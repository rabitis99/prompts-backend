package org.example.sharedprompts.domain.payment.domain.service;

import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentDomainException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentDomainService {

    public boolean canTransitionTo(Payment payment, PaymentStatus targetStatus) {
        return switch (payment.getStatus()) {
            case PENDING -> targetStatus == PaymentStatus.SUCCESS
                    || targetStatus == PaymentStatus.FAILED
                    || targetStatus == PaymentStatus.CANCELED;
            case SUCCESS -> targetStatus == PaymentStatus.CANCELED
                    || targetStatus == PaymentStatus.REFUNDED
                    || targetStatus == PaymentStatus.PARTIALLY_REFUNDED;
            case PARTIALLY_REFUNDED -> targetStatus == PaymentStatus.REFUNDED
                    || targetStatus == PaymentStatus.PARTIALLY_REFUNDED;
            case REFUND_IN_PROGRESS -> targetStatus == PaymentStatus.REFUNDED
                    || targetStatus == PaymentStatus.PARTIALLY_REFUNDED;
            default -> false;
        };
    }

    public void validateCanCancel(Payment payment) {
        if (!canCancel(payment)) {
            throw new PaymentDomainException(ErrorCode.PAYMENT_INVALID_STATUS, "status",
                "취소할 수 없는 결제 상태입니다.");
        }
    }

    public void validateCanRefund(Payment payment) {
        if (!payment.getStatus().isRefundable()) {
            throw new PaymentDomainException(ErrorCode.PAYMENT_INVALID_STATUS, "status",
                "환불할 수 없는 결제 상태입니다.");
        }
    }

    public void validateRefundAmount(Payment payment, BigDecimal refundAmount) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentDomainException(ErrorCode.PAYMENT_REFUND_AMOUNT_INVALID, "refundAmount");
        }

        BigDecimal refundableAmount = payment.getRefundableAmount();
        if (refundAmount.compareTo(refundableAmount) > 0) {
            throw new PaymentDomainException(ErrorCode.PAYMENT_REFUND_AMOUNT_EXCEEDED, "refundAmount");
        }
    }

    public boolean canCancel(Payment payment) {
        if (payment.getStatus() == PaymentStatus.PENDING) {
            return true;
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            BigDecimal refundedAmount = payment.getRefundedAmount();
            return refundedAmount == null || refundedAmount.compareTo(BigDecimal.ZERO) == 0;
        }
        return false;
    }

    public boolean canRefund(Payment payment) {
        boolean statusAllowsRefund = payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED;
        BigDecimal refundableAmount = payment.getRefundableAmount();
        if (refundableAmount == null) {
            return false;
        }
        return statusAllowsRefund && refundableAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean canRefundAmount(Payment payment, BigDecimal refundAmount) {
        if (refundAmount == null) {
            return false;
        }
        if (!canRefund(payment)) {
            return false;
        }
        BigDecimal refundableAmount = payment.getRefundableAmount();
        if (refundableAmount == null) {
            return false;
        }
        return refundAmount.compareTo(BigDecimal.ZERO) > 0
                && refundAmount.compareTo(refundableAmount) <= 0;
    }
}

