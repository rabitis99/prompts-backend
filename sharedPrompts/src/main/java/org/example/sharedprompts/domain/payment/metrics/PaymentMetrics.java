package org.example.sharedprompts.domain.payment.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 결제 관련 메트릭 수집
 * Prometheus 메트릭 수집을 위한 클래스
 */
@Component
@RequiredArgsConstructor
public class PaymentMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * Create a counter metric that tracks successful payments for the given payment method.
     *
     * @param paymentMethod the payment method value to attach as the `payment_method` tag
     * @return a Counter configured for the `payment.success` metric with the `payment_method` tag
     */
    private Counter paymentSuccessCounter(String paymentMethod) {
        return Counter.builder("payment.success")
                .tag("payment_method", paymentMethod)
                .description("결제 성공 횟수")
                .register(meterRegistry);
    }

    /**
     * Create and register a counter that tracks payment failures for a specific payment method and reason.
     *
     * @param paymentMethod the payment method value used for the `payment_method` tag
     * @param reason the failure reason used for the `reason` tag; if null, the tag value will be `"unknown"`
     * @return the registered Counter configured with `payment_method` and `reason` tags
     */
    private Counter paymentFailureCounter(String paymentMethod, String reason) {
        return Counter.builder("payment.failure")
                .tag("payment_method", paymentMethod)
                .tag("reason", reason != null ? reason : "unknown")
                .description("결제 실패 횟수")
                .register(meterRegistry);
    }

    /**
     * Returns a Timer for measuring payment processing time for the specified payment method.
     *
     * @param paymentMethod the payment method value used as the `payment_method` tag on the Timer
     * @return the Timer bound to the MeterRegistry with the `payment_method` tag set to the provided value
     */
    private Timer paymentProcessingTimer(String paymentMethod) {
        return Timer.builder("payment.processing.time")
                .tag("payment_method", paymentMethod)
                .description("결제 처리 시간")
                .register(meterRegistry);
    }

    /**
     * Record the payment amount as a gauge metric tagged by payment method.
     *
     * @param paymentMethod the payment method label to attach to the metric (e.g., "card", "paypal")
     * @param amount the payment amount to publish for the gauge
     */
    public void recordPaymentAmount(String paymentMethod, double amount) {
        meterRegistry.gauge("payment.amount", 
                Arrays.asList(Tag.of("payment_method", paymentMethod)), 
                amount);
    }

    /**
     * Records a successful payment event and its processing time for the given payment method.
     *
     * @param paymentMethod the payment method label used as the `payment_method` metric tag
     * @param processingTimeMs processing time in milliseconds to record in the `payment.processing.time` timer
     */
    public void recordPaymentSuccess(String paymentMethod, long processingTimeMs) {
        paymentSuccessCounter(paymentMethod).increment();
        paymentProcessingTimer(paymentMethod).record(processingTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record a payment failure metric and the processing time for the specified payment method.
     *
     * @param paymentMethod the payment method label to tag the metrics (e.g., "card", "paypal")
     * @param reason the failure reason tag; if null, the reason is recorded as "unknown"
     * @param processingTimeMs the processing time to record in milliseconds
     */
    public void recordPaymentFailure(String paymentMethod, String reason, long processingTimeMs) {
        paymentFailureCounter(paymentMethod, reason).increment();
        paymentProcessingTimer(paymentMethod).record(processingTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record a payment cancellation metric for the specified payment method.
     *
     * @param paymentMethod the value for the `payment_method` tag applied to the `payment.cancel` counter
     */
    public void recordPaymentCancel(String paymentMethod) {
        Counter.builder("payment.cancel")
                .tag("payment_method", paymentMethod)
                .description("결제 취소 횟수")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record refund-related metrics for a payment method.
     *
     * Increments the "payment.refund" counter and updates the "payment.refund.amount" gauge tagged by the given payment method.
     *
     * @param paymentMethod the payment method value used as the `payment_method` metric tag
     * @param refundAmount  the refunded amount (in monetary units) to record in the gauge
     */
    public void recordPaymentRefund(String paymentMethod, double refundAmount) {
        Counter.builder("payment.refund")
                .tag("payment_method", paymentMethod)
                .description("결제 환불 횟수")
                .register(meterRegistry)
                .increment();
        
        meterRegistry.gauge("payment.refund.amount",
                Arrays.asList(Tag.of("payment_method", paymentMethod)),
                refundAmount);
    }

    /**
     * Record an occurrence of a daily payment limit being exceeded.
     *
     * Records a counter metric named "payment.daily_limit_exceeded" tagged with the provided tier.
     *
     * @param userId identifier of the user triggering the limit (not used as a metric tag)
     * @param tier   subscription or account tier to tag the metric with
     */
    public void recordDailyLimitExceeded(Long userId, String tier) {
        Counter.builder("payment.daily_limit_exceeded")
                .tag("tier", tier)
                .description("일일 결제 제한 초과 횟수")
                .register(meterRegistry)
                .increment();
    }
}
