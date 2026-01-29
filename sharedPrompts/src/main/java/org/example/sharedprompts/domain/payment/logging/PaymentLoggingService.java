package org.example.sharedprompts.domain.payment.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 결제 관련 상세 로깅 및 추적 서비스
 * MDC를 사용한 트레이싱 ID 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentLoggingService {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String PAYMENT_ID_KEY = "paymentId";
    private static final String USER_ID_KEY = "userId";

    /**
     * Starts a tracing context by generating a trace identifier and placing trace, payment, and user IDs into MDC.
     *
     * @param paymentId the payment identifier to attach to the trace in MDC
     * @param userId the user identifier to attach to the trace in MDC
     * @return the generated trace identifier
     */
    public String startTrace(Long paymentId, Long userId) {
        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID_KEY, traceId);
        MDC.put(PAYMENT_ID_KEY, String.valueOf(paymentId));
        MDC.put(USER_ID_KEY, String.valueOf(userId));
        
        log.info("결제 트레이싱 시작: traceId={}, paymentId={}, userId={}", traceId, paymentId, userId);
        return traceId;
    }

    /**
     * Ends the current payment tracing context.
     *
     * Logs the trace termination including the active trace id and clears the MDC context.
     */
    public void endTrace() {
        String traceId = MDC.get(TRACE_ID_KEY);
        log.info("결제 트레이싱 종료: traceId={}", traceId);
        MDC.clear();
    }

    /**
     * Logs details of a payment request including payment id, user id, amount, currency, payment method, and tier.
     *
     * @param payment the Payment whose request details will be logged
     */
    public void logPaymentRequest(Payment payment) {
        log.info("결제 요청: paymentId={}, userId={}, amount={}, currency={}, paymentMethod={}, tier={}",
                payment.getId(),
                payment.getUser().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getTier());
    }

    /**
     * Log an informational message when an approval attempt is made for the given payment.
     *
     * @param payment  the payment for which an approval is being attempted
     * @param provider the external payment provider handling the approval attempt
     */
    public void logPaymentApprovalAttempt(Payment payment, String provider) {
        log.info("결제 승인 시도: paymentId={}, provider={}, externalPaymentId={}",
                payment.getId(),
                provider,
                payment.getExternalPaymentId());
    }

    /**
     * Log a successful payment approval.
     *
     * @param payment the Payment whose approval succeeded
     * @param externalPaymentId the external provider's payment identifier
     * @param processingTimeMs the time spent processing the approval in milliseconds
     */
    public void logPaymentApprovalSuccess(Payment payment, String externalPaymentId, long processingTimeMs) {
        log.info("결제 승인 성공: paymentId={}, externalPaymentId={}, processingTimeMs={}",
                payment.getId(),
                externalPaymentId,
                processingTimeMs);
    }

    /**
     * Log a payment approval failure including identifying details and the causing exception.
     *
     * Logs the payment's id and retry count, the provided failure reason, and the exception message and stack trace.
     *
     * @param payment the payment whose approval failed
     * @param reason  a brief explanation for why the approval failed
     * @param e       the exception that caused or accompanied the failure; its message and stack trace are logged
     */
    public void logPaymentApprovalFailure(Payment payment, String reason, Exception e) {
        log.error("결제 승인 실패: paymentId={}, reason={}, retryCount={}, error={}",
                payment.getId(),
                reason,
                payment.getRetryCount(),
                e.getMessage(),
                e);
    }

    /**
     * Log a payment's status transition including the external payment identifier.
     *
     * Records an informational log entry containing the payment id, the previous status,
     * the new status, and the payment's externalPaymentId.
     *
     * @param payment   the payment whose status changed
     * @param oldStatus the previous payment status
     * @param newStatus the new payment status
     */
    public void logPaymentStatusChange(Payment payment, PaymentStatus oldStatus, PaymentStatus newStatus) {
        log.info("결제 상태 변경: paymentId={}, oldStatus={}, newStatus={}, externalPaymentId={}",
                payment.getId(),
                oldStatus,
                newStatus,
                payment.getExternalPaymentId());
    }

    /**
     * Log a payment cancellation event.
     *
     * Logs an informational message containing the payment's id, the cancellation reason,
     * and the payment's external payment id.
     *
     * @param payment the payment that was cancelled
     * @param reason  the reason for the cancellation
     */
    public void logPaymentCancel(Payment payment, String reason) {
        log.info("결제 취소: paymentId={}, reason={}, externalPaymentId={}",
                payment.getId(),
                reason,
                payment.getExternalPaymentId());
    }

    /**
     * Log details about a payment refund.
     *
     * Logs the payment id, requested refund amount, reason, the payment's cumulative refunded amount,
     * and the remaining refundable amount.
     *
     * @param payment the payment being refunded
     * @param refundAmount the amount to refund
     * @param reason human-readable reason for the refund
     */
    public void logPaymentRefund(Payment payment, BigDecimal refundAmount, String reason) {
        log.info("결제 환불: paymentId={}, refundAmount={}, reason={}, refundedAmount={}, refundableAmount={}",
                payment.getId(),
                refundAmount,
                reason,
                payment.getRefundedAmount(),
                payment.getRefundableAmount());
    }

    /**
     * Logs a debug message about a user's daily payment count compared to the configured limit.
     *
     * @param userId     the ID of the user whose daily usage is being checked
     * @param tier       the user's tier or plan used to determine limits
     * @param todayCount the number of payments the user has made today
     * @param limit      the maximum allowed payments per day for the given tier
     */
    public void logDailyLimitCheck(Long userId, String tier, long todayCount, int limit) {
        log.debug("일일 결제 제한 체크: userId={}, tier={}, todayCount={}, limit={}",
                userId,
                tier,
                todayCount,
                limit);
    }

    /**
     * Log a warning when a user exceeds their daily payment limit.
     *
     * @param userId     the identifier of the user whose limit was exceeded
     * @param tier       the user's tier or subscription level
     * @param todayCount the number of payments the user has made today
     * @param limit      the allowed daily payment limit
     */
    public void logDailyLimitExceeded(Long userId, String tier, long todayCount, int limit) {
        log.warn("일일 결제 제한 초과: userId={}, tier={}, todayCount={}, limit={}",
                userId,
                tier,
                todayCount,
                limit);
    }

    /**
     * Logs an informational entry for a payment retry attempt.
     *
     * @param payment       the payment being retried
     * @param attemptNumber the retry attempt sequence number (1 for first retry)
     */
    public void logRetryAttempt(Payment payment, int attemptNumber) {
        log.info("결제 재시도: paymentId={}, attemptNumber={}, retryCount={}",
                payment.getId(),
                attemptNumber,
                payment.getRetryCount());
    }

    /**
     * Logs receipt of a webhook including the originating provider, event type, and payload length.
     *
     * @param provider the originating webhook provider
     * @param eventType the webhook event type
     * @param payload the raw webhook payload, may be null
     */
    public void logWebhookReceived(String provider, String eventType, String payload) {
        log.info("Webhook 수신: provider={}, eventType={}, payloadLength={}",
                provider,
                eventType,
                payload != null ? payload.length() : 0);
    }

    /**
     * Log an error for a failed webhook processing event.
     *
     * @param provider  the name of the webhook provider (e.g., "stripe", "paypal")
     * @param eventType the webhook event type or topic received
     * @param e         the exception that caused the processing failure; its message and stack trace will be logged
     */
    public void logWebhookProcessingFailure(String provider, String eventType, Exception e) {
        log.error("Webhook 처리 실패: provider={}, eventType={}, error={}",
                provider,
                eventType,
                e.getMessage(),
                e);
    }
}
