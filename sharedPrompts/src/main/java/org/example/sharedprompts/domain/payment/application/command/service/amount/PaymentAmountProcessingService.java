package org.example.sharedprompts.domain.payment.application.command.service.amount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.dto.AmountProcessingResult;
import org.example.sharedprompts.domain.payment.domain.service.PaymentAmountCalculator;
import org.example.sharedprompts.domain.payment.domain.valueobject.PaymentAmount;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAmountProcessingService {

    private final PaymentAmountValidator validator;
    private final CurrencyConverter currencyConverter;
    private final PointProcessor pointProcessor;
    private final PaymentAmountCalculator amountCalculator;

    public AmountProcessingResult processPaymentAmount(Long userId, BigDecimal originalAmount,
                                                       String currencyCode, BigDecimal usePointAmount) {
        log.debug("결제 금액 처리 시작: userId={}, amount={}, currency={}, usePoint={}", 
                userId, originalAmount, currencyCode, usePointAmount);

        // 입력 검증
        validator.validatePaymentInputs(userId, originalAmount, currencyCode);

        // 원본 금액 생성
        PaymentAmount originalPaymentAmount = PaymentAmount.of(originalAmount, currencyCode);

        // 환율 변환
        PaymentAmount convertedPaymentAmount = currencyConverter.convertToKrw(originalPaymentAmount);

        // 포인트 금액 생성
        PaymentAmount usePointPaymentAmount = createUsePointAmount(usePointAmount);

        // 실제 결제 금액 계산
        PaymentAmount actualPaymentAmount = calculateActualPaymentAmount(
                userId, usePointPaymentAmount, convertedPaymentAmount);

        log.info("결제 금액 처리 완료: userId={}, original={}, converted={}, usePoint={}, actual={}",
                userId, originalPaymentAmount, convertedPaymentAmount, usePointPaymentAmount, actualPaymentAmount);

        return new AmountProcessingResult(
                originalPaymentAmount.toBigDecimal(),
                convertedPaymentAmount.toBigDecimal(),
                usePointPaymentAmount.toBigDecimal(),
                actualPaymentAmount.toBigDecimal()
        );
    }

    public BigDecimal calculateRefundPointAmount(BigDecimal usedPointAmount, BigDecimal originalAmount,
                                                 BigDecimal refundAmount) {
        return amountCalculator.calculateRefundPointAmount(usedPointAmount, originalAmount, refundAmount);
    }

    public BigDecimal calculateActualAmount(BigDecimal totalAmount, BigDecimal usedPointAmount) {
        validator.validateAmount(totalAmount);

        PaymentAmount total = PaymentAmount.krw(totalAmount);
        PaymentAmount used = createUsePointAmount(usedPointAmount);

        return amountCalculator.calculateActualAmount(total, used).toBigDecimal();
    }

    private PaymentAmount createUsePointAmount(BigDecimal usePointAmount) {
        if (usePointAmount == null) {
            return PaymentAmount.krw(BigDecimal.ZERO);
        }
        if (usePointAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("usePointAmount는 0 이상이어야 합니다");
        }
        if (usePointAmount.compareTo(BigDecimal.ZERO) == 0) {
            return PaymentAmount.krw(BigDecimal.ZERO);
        }
        return PaymentAmount.krw(usePointAmount);
    }

    private PaymentAmount calculateActualPaymentAmount(Long userId, PaymentAmount usePointPaymentAmount, 
                                                       PaymentAmount convertedPaymentAmount) {
        if (!usePointPaymentAmount.isPositive()) {
            return convertedPaymentAmount;
        }

        PaymentAmount actualUseAmount = pointProcessor.validateAndUsePoints(
                userId, usePointPaymentAmount, convertedPaymentAmount);

        return amountCalculator.calculateActualAmount(convertedPaymentAmount, actualUseAmount);
    }
}

