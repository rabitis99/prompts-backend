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

        /**
         * Creates a result container for payment amount processing.
         *
         * @param originalAmount      the amount provided in the payment request (in the request currency)
         * @param convertedAmount     the amount after currency conversion (KRW when conversion was applied)
         * @param usedPointAmount     the amount of points applied to the payment
         * @param actualPaymentAmount the final payable amount after applying points
         */
        public AmountProcessingResult(BigDecimal originalAmount, BigDecimal convertedAmount, 
                                     BigDecimal usedPointAmount, BigDecimal actualPaymentAmount) {
            this.originalAmount = originalAmount;
            this.convertedAmount = convertedAmount;
            this.usedPointAmount = usedPointAmount;
            this.actualPaymentAmount = actualPaymentAmount;
        }

        /**
         * The original payment amount as supplied in the request.
         *
         * @return the original amount from the payment request.
         */
        public BigDecimal getOriginalAmount() {
            return originalAmount;
        }

        /**
         * The payment amount after currency conversion (to KRW when conversion was required).
         *
         * @return the converted amount
         */
        public BigDecimal getConvertedAmount() {
            return convertedAmount;
        }

        /**
         * The amount of points applied to the payment.
         *
         * @return the amount of points applied to the payment
         */
        public BigDecimal getUsedPointAmount() {
            return usedPointAmount;
        }

        /**
         * Get the final payment amount after currency conversion and point deduction.
         *
         * @return the final payable amount (in KRW) after applied points
         */
        public BigDecimal getActualPaymentAmount() {
            return actualPaymentAmount;
        }
    }

    /**
     * Convert the requested amount to KRW if necessary and apply user points to determine the final payable amount.
     *
     * Processes currency conversion, validates and consumes requested points (capped to the converted amount), and
     * computes the actual payment amount after point deduction.
     *
     * @param userId  the identifier of the user making the payment
     * @param request the payment request containing amount, currency, and optional usePointAmount
     * @return an AmountProcessingResult containing originalAmount, convertedAmount (in KRW when conversion occurred),
     *         usedPointAmount, and actualPaymentAmount
     * @throws ApiException if the user does not have enough point balance to cover the requested usePointAmount
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
     * Calculate how many points should be refunded for a given refund amount.
     *
     * If no points were used, returns BigDecimal.ZERO. For full refunds (refundAmount >= originalAmount)
     * returns the entire usedPointAmount. For partial refunds returns the proportional share of used points
     * based on refundAmount/originalAmount, rounded down to a whole point.
     *
     * @param usedPointAmount the amount of points originally applied to the payment
     * @param originalAmount the original payment amount
     * @param refundAmount the amount being refunded
     * @return the amount of points to refund (whole points), or `BigDecimal.ZERO` if no points were used
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
