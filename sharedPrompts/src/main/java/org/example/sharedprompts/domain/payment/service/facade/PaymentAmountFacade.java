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
 *
 * <p><strong>알려진 제한사항:</strong>
 * - 포인트는 requestPayment 시점에 차감됨 (결제 요청 단계)
 * - 실제 결제 승인은 confirmPayment에서 발생
 * - 사용자가 결제를 포기하면 포인트가 차감된 채로 남을 수 있음
 *
 * <p><strong>권장 개선사항:</strong>
 * - 스케줄러를 통해 PENDING 상태가 일정 시간(예: 30분) 경과한 결제를 자동 만료 처리
 * - 만료된 결제의 사용 포인트를 자동으로 복구
 * - 또는 포인트 사용 시점을 confirmPayment로 이동 (아키텍처 변경 필요)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentAmountFacade {

    private final ExchangeRateService exchangeRateService;
    private final PointService pointService;

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
     *
     * <p>파라미터 null 체크를 일관성 있게 수행합니다.
     * 모든 파라미터가 null이거나 0 이하인 경우를 처리합니다.
     *
     * @param usedPointAmount 사용한 포인트 금액 (nullable)
     * @param originalAmount 원래 결제 금액 (nullable)
     * @param refundAmount 환불 금액 (required, null일 경우 예외)
     * @return 환불할 포인트 금액
     * @throws ApiException refundAmount가 null이거나 0 이하인 경우
     */
    public BigDecimal calculateRefundPointAmount(BigDecimal usedPointAmount, BigDecimal originalAmount, BigDecimal refundAmount) {
        // refundAmount가 null이거나 0 이하일 경우 조기에 차단 (필수 파라미터)
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.PAYMENT_REFUND_AMOUNT_INVALID);
        }

        // usedPointAmount null 체크 추가 (일관성)
        if (usedPointAmount == null || usedPointAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // originalAmount null 체크 추가 (일관성)
        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
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

