package org.example.sharedprompts.domain.payment.service.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.metrics.PaymentMetrics;
import org.example.sharedprompts.domain.payment.service.cashback.CashbackService;
import org.example.sharedprompts.domain.payment.service.evnet.PaymentEventPublisher;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 결제 후처리 파사드
 * 포인트/캐시백 적립, 이벤트 발행, 메트릭 수집, 로깅을 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentPostProcessFacade {

    private final PointService pointService;
    private final CashbackService cashbackService;
    private final PaymentEventPublisher eventPublisher;
    private final PaymentMetrics paymentMetrics;
    private final PaymentLoggingService loggingService;

    /**
     * Perform post-processing steps for a successful payment.
     *
     * Coordinates logging of approval and status change, accrues points and cashback,
     * records success and amount metrics, and publishes a payment-succeeded event.
     *
     * @param payment the completed Payment entity
     * @param userId the identifier of the user who made the payment
     * @param actualPaymentAmount the actual charged amount used for point accrual
     * @param originalAmount the original payment amount used for cashback accrual and amount metrics
     * @param processingTime the payment processing duration in milliseconds
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
     * Handle post-processing after a payment failure: log the failure, record metrics, and publish a failure event.
     *
     * @param payment      the Payment that failed
     * @param userId       the ID of the user associated with the payment; may be null if unknown
     * @param errorMessage a human-readable error message describing the failure
     * @param exception    the exception that caused or accompanied the failure, if available
     * @param processingTime time taken (in milliseconds) to process the payment attempt
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
     * Handle post-processing for a canceled payment.
     *
     * <p>If the payment used points, refunds those points to the user, records cancellation metrics,
     * logs the cancellation and the status transition, and publishes a payment-canceled event.</p>
     *
     * @param payment   the payment that was canceled
     * @param userId    the id of the user associated with the payment
     * @param reason    the reason for the cancellation
     * @param oldStatus the payment status prior to cancellation
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
     * Handle post-processing after a payment refund.
     *
     * Performs point refunds when applicable, records refund metrics, logs the refund and status change, and publishes a payment-refunded event.
     *
     * @param payment           the payment being refunded
     * @param userId            the ID of the user receiving the refund
     * @param refundAmount      the monetary amount refunded
     * @param refundPointAmount the amount of points to refund to the user
     * @param reason            the reason for the refund
     * @param oldStatus         the payment status before the refund was applied
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
