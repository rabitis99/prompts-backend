package org.example.sharedprompts.domain.payment.domain.service;

import org.example.sharedprompts.domain.payment.domain.valueobject.ExchangeRate;
import org.example.sharedprompts.domain.payment.domain.valueobject.PaymentAmount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PaymentAmountCalculator {

    public PaymentAmount convertCurrency(PaymentAmount originalAmount, ExchangeRate exchangeRate) {
        return exchangeRate.convert(originalAmount);
    }

    public PaymentAmount calculateActualAmount(PaymentAmount convertedAmount, PaymentAmount usePointAmount) {
        if (usePointAmount.isZero() || !usePointAmount.isPositive()) {
            return convertedAmount;
        }

        if (usePointAmount.isGreaterThan(convertedAmount)) {
            return PaymentAmount.krw(BigDecimal.ZERO);
        }

        return convertedAmount.subtract(usePointAmount);
    }

    public BigDecimal calculateRefundPointAmount(BigDecimal usedPointAmount, BigDecimal originalAmount, BigDecimal refundAmount) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }

        if (usedPointAmount == null || usedPointAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("원래 결제 금액이 유효하지 않습니다.");
        }

        PaymentAmount refundPaymentAmount = PaymentAmount.krw(refundAmount);
        PaymentAmount usedPointPaymentAmount = PaymentAmount.krw(usedPointAmount);
        PaymentAmount originalPaymentAmount = PaymentAmount.krw(originalAmount);

        if (refundPaymentAmount.compareTo(originalPaymentAmount) >= 0) {
            return usedPointPaymentAmount.toBigDecimal();
        } else {
            BigDecimal refundRatio = refundPaymentAmount.toBigDecimal()
                .divide(originalPaymentAmount.toBigDecimal(), 4, RoundingMode.HALF_UP);
            PaymentAmount refundPointAmount = usedPointPaymentAmount.multiply(refundRatio);
            return refundPointAmount.toBigDecimal().setScale(0, RoundingMode.DOWN);
        }
    }
}

