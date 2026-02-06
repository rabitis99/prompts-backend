package org.example.sharedprompts.domain.payment.application.command.service.confirm;

import lombok.Getter;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;

import java.math.BigDecimal;

@Getter
public class PaymentExecutionResult {
    private final Payment payment;
    private final long processingTime;
    private final boolean succeeded;
    private final BigDecimal actualAmount;
    private final BigDecimal originalAmount;

    public PaymentExecutionResult(Payment payment, long processingTime, boolean succeeded,
                                 BigDecimal actualAmount, BigDecimal originalAmount) {
        this.payment = payment;
        this.processingTime = processingTime;
        this.succeeded = succeeded;
        this.actualAmount = actualAmount;
        this.originalAmount = originalAmount;
    }

}

