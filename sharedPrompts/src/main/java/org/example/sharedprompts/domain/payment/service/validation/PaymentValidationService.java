package org.example.sharedprompts.domain.payment.service.validation;

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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 검증 서비스
 * 
 * <p>단일 책임: 결제 관련 모든 검증만 담당
 * - 일일 결제 제한 검증
 * - 소유권 검증
 * - 상태 검증 (취소/환불 가능 여부)
 * - 환불 금액 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentValidationService {

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
     * 결제 취소 가능 상태 체크 (비즈니스 검증 메서드 사용)
     *
     * @throws ApiException 취소 불가능한 상태일 경우
     */
    public void validateCancelableStatus(Payment payment) {
        if (!validateCanCancelPayment(payment)) {
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

    // ===== 상태 전이 검증 메서드 (엔티티에서 이동) =====

    /**
     * 특정 상태로 전이 가능한지 확인
     *
     * <p>결제 상태 전이 규칙:
     * - PENDING → SUCCESS, FAILED, CANCELED
     * - SUCCESS → CANCELED, REFUNDED, PARTIALLY_REFUNDED
     * - PARTIALLY_REFUNDED → REFUNDED, PARTIALLY_REFUNDED (추가 환불)
     * - FAILED, CANCELED, REFUNDED → 전이 불가
     *
     * @param payment 결제 엔티티
     * @param targetStatus 전이할 목표 상태
     * @return 전이 가능 여부
     */
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
            case FAILED, CANCELED, REFUNDED -> false; // 최종 상태에서는 전이 불가
        };
    }

    /**
     * 취소 가능 여부 확인 (기존 메서드 개선)
     *
     * <p>취소 가능 조건:
     * - SUCCESS 상태이고 환불된 금액이 없음
     * - PENDING 상태 (결제 진행 중 취소)
     *
     * @param payment 결제 엔티티
     * @return 취소 가능 여부
     */
    public boolean validateCanCancelPayment(Payment payment) {
        if (payment.getStatus() == PaymentStatus.PENDING) {
            return true;
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment.getRefundedAmount().compareTo(BigDecimal.ZERO) == 0;
        }
        return false;
    }

    /**
     * 환불 가능 여부 확인 (기존 메서드 개선)
     *
     * <p>환불 가능 조건:
     * - SUCCESS 상태이거나 PARTIALLY_REFUNDED 상태
     * - 환불 가능 금액이 남아있음
     *
     * @param payment 결제 엔티티
     * @return 환불 가능 여부
     */
    public boolean validateCanRefundPayment(Payment payment) {
        boolean statusAllowsRefund = payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED;
        boolean hasRefundableAmount = payment.getRefundableAmount().compareTo(BigDecimal.ZERO) > 0;
        return statusAllowsRefund && hasRefundableAmount;
    }

    /**
     * 특정 금액 환불 가능 여부 확인
     *
     * @param payment 결제 엔티티
     * @param refundAmount 환불할 금액
     * @return 환불 가능 여부
     */
    public boolean validateCanRefundPaymentAmount(Payment payment, BigDecimal refundAmount) {
        if (!validateCanRefundPayment(payment)) {
            return false;
        }
        return refundAmount.compareTo(BigDecimal.ZERO) > 0
                && refundAmount.compareTo(payment.getRefundableAmount()) <= 0;
    }

    /**
     * 결제가 만료되었는지 확인
     *
     * <p>PENDING 상태에서 일정 시간 경과 시 만료로 간주
     * 만료된 결제는 자동으로 취소되고 사용한 포인트가 복구되어야 함
     *
     * @param payment 결제 엔티티
     * @param expiryMinutes 만료 시간 (분)
     * @return 만료 여부
     */
    public boolean isPaymentExpired(Payment payment, long expiryMinutes) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return false;
        }
        LocalDateTime expiryTime = payment.getCreatedAt().plusMinutes(expiryMinutes);
        return LocalDateTime.now().isAfter(expiryTime);
    }

    /**
     * 포인트를 사용했지만 결제가 완료되지 않은 상태인지 확인
     *
     * <p>이 상태의 결제는 포인트 복구가 필요할 수 있음
     * - PENDING 상태이고 포인트를 사용했으면 사용자가 결제를 포기했을 가능성
     * - 스케줄러를 통해 일정 시간 후 자동으로 포인트 복구 처리 권장
     *
     * @param payment 결제 엔티티
     * @return 포인트 복구 필요 여부
     */
    public boolean hasUnrecoveredPoints(Payment payment) {
        return payment.getStatus() == PaymentStatus.PENDING
                && payment.getUsedPointAmount() != null
                && payment.getUsedPointAmount().compareTo(BigDecimal.ZERO) > 0;
    }
}

