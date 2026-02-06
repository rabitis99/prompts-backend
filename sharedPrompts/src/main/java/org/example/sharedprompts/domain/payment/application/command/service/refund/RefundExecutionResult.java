package org.example.sharedprompts.domain.payment.application.command.service.refund;

import org.example.sharedprompts.domain.payment.domain.entity.Payment;

import java.math.BigDecimal;

public class RefundExecutionResult {
    private final Payment refundedPayment;
    private final BigDecimal refundAmount;

    public RefundExecutionResult(Payment refundedPayment, BigDecimal refundAmount) {
        this.refundedPayment = refundedPayment;
        this.refundAmount = refundAmount;
    }

    public Payment getRefundedPayment() {
        return refundedPayment;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }
}

