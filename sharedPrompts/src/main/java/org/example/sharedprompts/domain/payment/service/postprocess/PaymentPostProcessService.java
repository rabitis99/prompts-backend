package org.example.sharedprompts.domain.payment.service.postprocess;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.metrics.PaymentMetrics;
import org.example.sharedprompts.domain.payment.service.cashback.CashbackService;
import org.example.sharedprompts.domain.payment.service.event.PaymentEventPublisher;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 결제 후처리 서비스
 * 
 * <p>단일 책임: 결제 후처리만 담당
 * - 포인트/캐시백 적립
 * - 이벤트 발행
 * - 메트릭 수집
 * - 로깅
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPostProcessService {

    private final PointService pointService;
    private final CashbackService cashbackService;
    private final PaymentEventPublisher eventPublisher;
    private final PaymentMetrics paymentMetrics;
    private final PaymentLoggingService loggingService;

    /**
     * 결제 성공 후처리
     */
    public void processPaymentSuccess(Payment payment, Long userId, BigDecimal actualPaymentAmount, 
                                     BigDecimal originalAmount, long processingTime) {
        // 로깅
        loggingService.logPaymentApprovalSuccess(payment, payment.getExternalPaymentId(), processingTime);
        loggingService.logPaymentStatusChange(payment, PaymentStatus.PENDING, PaymentStatus.SUCCESS);

        // 포인트 적립 (실제 결제 금액 기준)
        pointService.accumulatePoints(userId, payment.getId(), actualPaymentAmount);

        // 캐시백 적립 (원래 결제 금액 기준)
        cashbackService.accumulateCashback(userId, payment.getId(), originalAmount);

        // 메트릭 기록
        paymentMetrics.recordPaymentSuccess(payment.getPaymentMethod().name(), processingTime);
        paymentMetrics.recordPaymentAmount(payment.getPaymentMethod().name(), originalAmount.doubleValue());

        // 결제 성공 이벤트 발행
        eventPublisher.publishPaymentSucceeded(payment.getId(), userId, payment.getPaymentMethod().name());
    }

    /**
     * 결제 실패 후처리
     */
    public void processPaymentFailure(Payment payment, Long userId, String errorMessage, 
                                     Exception exception, long processingTime) {
        // 로깅
        loggingService.logPaymentApprovalFailure(payment, errorMessage, exception);
        
        // 메트릭 기록
        paymentMetrics.recordPaymentFailure(payment.getPaymentMethod().name(), errorMessage, processingTime);
        
        // 결제 실패 이벤트 발행
        eventPublisher.publishPaymentFailed(
                payment.getId(),
                userId,
                errorMessage,
                payment.getPaymentMethod().name()
        );
    }

    /**
     * 결제 취소 후처리
     */
    public void processPaymentCancel(Payment payment, Long userId, String reason, PaymentStatus oldStatus) {
        // 포인트 환불 처리
        if (payment.getUsedPointAmount().compareTo(BigDecimal.ZERO) > 0) {
            pointService.accumulatePoints(userId, payment.getId(), payment.getUsedPointAmount());
            log.info("결제 취소로 인한 포인트 환불: userId={}, paymentId={}, refundPointAmount={}", 
                    userId, payment.getId(), payment.getUsedPointAmount());
        }

        // 로깅
        loggingService.logPaymentCancel(payment, reason);
        loggingService.logPaymentStatusChange(payment, oldStatus, PaymentStatus.CANCELED);
        
        // 메트릭 기록
        paymentMetrics.recordPaymentCancel(payment.getPaymentMethod().name());

        // 결제 취소 이벤트 발행
        eventPublisher.publishPaymentCanceled(payment.getId(), userId, reason);
    }

    /**
     * 결제 환불 후처리
     */
    public void processPaymentRefund(Payment payment, Long userId, BigDecimal refundAmount, 
                                    BigDecimal refundPointAmount, String reason, PaymentStatus oldStatus) {
        // 포인트 환불 처리
        if (refundPointAmount.compareTo(BigDecimal.ZERO) > 0) {
            pointService.accumulatePoints(userId, payment.getId(), refundPointAmount);
            log.info("포인트 환불: userId={}, paymentId={}, refundPointAmount={}", 
                    userId, payment.getId(), refundPointAmount);
        }

        // 로깅
        loggingService.logPaymentRefund(payment, refundAmount, reason);
        loggingService.logPaymentStatusChange(payment, oldStatus, payment.getStatus());
        
        // 메트릭 기록
        paymentMetrics.recordPaymentRefund(payment.getPaymentMethod().name(), refundAmount.doubleValue());

        // 결제 환불 이벤트 발행
        eventPublisher.publishPaymentRefunded(payment.getId(), userId, reason);
    }
}

