package org.example.sharedprompts.domain.payment.service.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.service.exchange.ExchangeRateService;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 결제 금액 처리 파사드
 * 환율 변환, 포인트 사용, 실제 결제 금액 계산을 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentAmountFacade {

    private final ExchangeRateService exchangeRateService;
    private final PointService pointService;

    /**
     * 결제 금액 처리 결과
     */
    public static class AmountProcessingResult {
        private final BigDecimal originalAmount;
        private final BigDecimal convertedAmount;
        private final BigDecimal usedPointAmount;
        private final BigDecimal actualPaymentAmount;

        public AmountProcessingResult(BigDecimal originalAmount, BigDecimal convertedAmount, 
                                     BigDecimal usedPointAmount, BigDecimal actualPaymentAmount) {
            this.originalAmount = originalAmount;
            this.convertedAmount = convertedAmount;
            this.usedPointAmount = usedPointAmount;
            this.actualPaymentAmount = actualPaymentAmount;
        }

        public BigDecimal getOriginalAmount() {
            return originalAmount;
        }

        public BigDecimal getConvertedAmount() {
            return convertedAmount;
        }

        public BigDecimal getUsedPointAmount() {
            return usedPointAmount;
        }

        public BigDecimal getActualPaymentAmount() {
            return actualPaymentAmount;
        }
    }

    /**
     * 결제 금액 처리 (환율 변환 + 포인트 사용)
     */
    public AmountProcessingResult processPaymentAmount(Long userId, PaymentRequestDto request) {
        BigDecimal originalAmount = request.getAmount();
        
        // 통화 변환 (필요한 경우)
        BigDecimal convertedAmount = originalAmount;
        if (!"KRW".equals(request.getCurrency())) {
            convertedAmount = exchangeRateService.convertCurrency(
                    originalAmount,
                    request.getCurrency(),
                    "KRW"
            );
        }

        // 포인트 사용 처리
        BigDecimal usePointAmount = request.getUsePointAmount() != null ? request.getUsePointAmount() : BigDecimal.ZERO;
        BigDecimal actualPaymentAmount = convertedAmount;
        
        if (usePointAmount.compareTo(BigDecimal.ZERO) > 0) {
            // 포인트 잔액 확인
            BigDecimal currentBalance = pointService.getCurrentBalance(userId);
            if (currentBalance.compareTo(usePointAmount) < 0) {
                throw new ApiException(ErrorCode.POINT_INSUFFICIENT);
            }
            
            // 포인트 사용 (실제 결제 금액에서 차감)
            if (usePointAmount.compareTo(convertedAmount) > 0) {
                // 포인트가 결제 금액보다 큰 경우, 결제 금액만큼만 사용
                usePointAmount = convertedAmount;
            }
            
            pointService.usePoints(userId, usePointAmount, "결제 시 포인트 사용");
            actualPaymentAmount = convertedAmount.subtract(usePointAmount);
            
            log.info("포인트 사용: userId={}, usePointAmount={}, originalAmount={}, actualPaymentAmount={}", 
                    userId, usePointAmount, convertedAmount, actualPaymentAmount);
        }

        return new AmountProcessingResult(originalAmount, convertedAmount, usePointAmount, actualPaymentAmount);
    }

    /**
     * 환불 시 포인트 환불 금액 계산
     */
    public BigDecimal calculateRefundPointAmount(BigDecimal usedPointAmount, BigDecimal originalAmount, BigDecimal refundAmount) {
        if (usedPointAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // 전체 환불인 경우 사용한 포인트를 다시 적립
        if (refundAmount.compareTo(originalAmount) >= 0) {
            // 전체 환불: 사용한 포인트 전액 환불
            return usedPointAmount;
        } else {
            // 부분 환불: 비율에 따라 포인트 환불
            BigDecimal refundRatio = refundAmount.divide(originalAmount, 4, RoundingMode.HALF_UP);
            return usedPointAmount.multiply(refundRatio)
                    .setScale(0, RoundingMode.DOWN);
        }
    }
}

