package org.example.sharedprompts.domain.payment.service.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.enums.UserTier;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.metrics.PaymentMetrics;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 결제 검증 파사드
 * 티어 체크, 권한 체크, 상태 체크 등의 검증 로직을 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentValidationFacade {

    private final PaymentRepository paymentRepository;
    private final PaymentLoggingService loggingService;
    private final PaymentMetrics paymentMetrics;

    /**
     * 일일 결제 제한 체크
     */
    public void validateDailyLimit(Long userId, UserTier tier) {
        long todayPaymentCount = paymentRepository.countTodaySuccessfulPayments(userId, PaymentStatus.SUCCESS);
        
        loggingService.logDailyLimitCheck(userId, tier.name(), todayPaymentCount, tier.getDailyLimit());
        
        if (todayPaymentCount >= tier.getDailyLimit()) {
            loggingService.logDailyLimitExceeded(userId, tier.name(), todayPaymentCount, tier.getDailyLimit());
            paymentMetrics.recordDailyLimitExceeded(userId, tier.name());
            throw new ApiException(ErrorCode.PAYMENT_DAILY_LIMIT_EXCEEDED);
        }
    }

    /**
     * 결제 권한 체크
     */
    public void validatePaymentOwnership(Payment payment, Long userId) {
        if (!payment.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.PAYMENT_FORBIDDEN);
        }
    }

    /**
     * 결제 취소 가능 상태 체크
     */
    public void validateCancelableStatus(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ApiException(ErrorCode.PAYMENT_INVALID_STATUS);
        }
    }

    /**
     * 결제 환불 가능 상태 체크
     */
    public void validateRefundableStatus(Payment payment) {
        if (!payment.getStatus().isRefundable()) {
            throw new ApiException(ErrorCode.PAYMENT_INVALID_STATUS);
        }
    }

    /**
     * 환불 금액 검증
     */
    public BigDecimal validateRefundAmount(BigDecimal requestedAmount, Payment payment) {
        BigDecimal refundAmount = requestedAmount;
        if (refundAmount == null) {
            refundAmount = payment.getRefundableAmount();
        }

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.PAYMENT_REFUND_AMOUNT_INVALID);
        }

        if (refundAmount.compareTo(payment.getRefundableAmount()) > 0) {
            throw new ApiException(ErrorCode.PAYMENT_REFUND_AMOUNT_EXCEEDED);
        }

        return refundAmount;
    }
}

