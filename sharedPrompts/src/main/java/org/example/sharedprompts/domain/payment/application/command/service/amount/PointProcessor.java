package org.example.sharedprompts.domain.payment.application.command.service.amount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.valueobject.PaymentAmount;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointProcessor {

    private final PointService pointService;

    public PaymentAmount validateAndUsePoints(Long userId, PaymentAmount usePointAmount, PaymentAmount paymentAmount) {
        validatePointBalance(userId, usePointAmount);
        PaymentAmount actualUseAmount = calculateActualUseAmount(usePointAmount, paymentAmount);
        usePoints(userId, actualUseAmount, paymentAmount);
        return actualUseAmount;
    }

    private void validatePointBalance(Long userId, PaymentAmount usePointAmount) {
        BigDecimal currentBalance = pointService.getCurrentBalance(userId);
        PaymentAmount currentBalanceAmount = PaymentAmount.krw(currentBalance);

        if (currentBalanceAmount.isLessThan(usePointAmount)) {
            log.warn("포인트 부족: userId={}, 요청={}, 보유={}", 
                    userId, usePointAmount, currentBalanceAmount);
            throw new ApiException(ErrorCode.POINT_INSUFFICIENT);
        }
    }

    private PaymentAmount calculateActualUseAmount(PaymentAmount usePointAmount, PaymentAmount paymentAmount) {
        return usePointAmount.isGreaterThan(paymentAmount) 
                ? paymentAmount 
                : usePointAmount;
    }

    private void usePoints(Long userId, PaymentAmount actualUseAmount, PaymentAmount paymentAmount) {
        pointService.usePoints(userId, actualUseAmount.toBigDecimal(), "결제 시 포인트 사용");
        log.info("포인트 사용 완료: userId={}, 사용금액={}, 결제금액={}", 
                userId, actualUseAmount, paymentAmount);
    }
}

