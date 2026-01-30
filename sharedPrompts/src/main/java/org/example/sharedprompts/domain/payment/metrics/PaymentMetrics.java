package org.example.sharedprompts.domain.payment.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 결제 관련 메트릭 수집
 * Prometheus 메트릭 수집을 위한 클래스
 */
@RequiredArgsConstructor
public class PaymentMetrics {

    private final MeterRegistry meterRegistry;

    // paymentMethod 별 결제 금액 상태 저장
    private final ConcurrentMap<String, AtomicReference<Double>> paymentAmountGauges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicReference<Double>> refundAmountGauges = new ConcurrentHashMap<>();

    // 결제 성공/실패 카운터
    private Counter paymentSuccessCounter(String paymentMethod) {
        return Counter.builder("payment.success")
                .tag("payment_method", paymentMethod)
                .description("결제 성공 횟수")
                .register(meterRegistry);
    }

    private Counter paymentFailureCounter(String paymentMethod, String reason) {
        return Counter.builder("payment.failure")
                .tag("payment_method", paymentMethod)
                .tag("reason", reason != null ? reason : "unknown")
                .description("결제 실패 횟수")
                .register(meterRegistry);
    }

    // 결제 처리 시간 타이머
    private Timer paymentProcessingTimer(String paymentMethod) {
        return Timer.builder("payment.processing.time")
                .tag("payment_method", paymentMethod)
                .description("결제 처리 시간")
                .register(meterRegistry);
    }

    // 결제 금액 게이지 (AtomicReference 사용)
    public void recordPaymentAmount(String paymentMethod, double amount) {
        AtomicReference<Double> ref = paymentAmountGauges.computeIfAbsent(paymentMethod, pm -> {
            AtomicReference<Double> r = new AtomicReference<>(0.0);
            Gauge.builder("payment.amount", r, AtomicReference::get)
                    .tag("payment_method", pm)
                    .register(meterRegistry);
            return r;
        });
        ref.set(amount);
    }

    /**
     * 결제 성공 메트릭 기록
     */
    public void recordPaymentSuccess(String paymentMethod, long processingTimeMs) {
        paymentSuccessCounter(paymentMethod).increment();
        paymentProcessingTimer(paymentMethod).record(processingTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 결제 실패 메트릭 기록
     */
    public void recordPaymentFailure(String paymentMethod, String reason, long processingTimeMs) {
        paymentFailureCounter(paymentMethod, reason).increment();
        paymentProcessingTimer(paymentMethod).record(processingTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 결제 취소 메트릭 기록
     */
    public void recordPaymentCancel(String paymentMethod) {
        Counter.builder("payment.cancel")
                .tag("payment_method", paymentMethod)
                .description("결제 취소 횟수")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 결제 환불 메트릭 기록 (AtomicReference 사용)
     */
    public void recordPaymentRefund(String paymentMethod, double refundAmount) {
        Counter.builder("payment.refund")
                .tag("payment_method", paymentMethod)
                .description("결제 환불 횟수")
                .register(meterRegistry)
                .increment();

        AtomicReference<Double> ref = refundAmountGauges.computeIfAbsent(paymentMethod, pm -> {
            AtomicReference<Double> r = new AtomicReference<>(0.0);
            Gauge.builder("payment.refund.amount", r, AtomicReference::get)
                    .tag("payment_method", pm)
                    .register(meterRegistry);
            return r;
        });
        ref.set(refundAmount);
    }

    /**
     * 일일 결제 제한 초과 메트릭 기록
     */
    public void recordDailyLimitExceeded(Long userId, String tier) {
        Counter.builder("payment.daily_limit_exceeded")
                .tag("tier", tier)
                .description("일일 결제 제한 초과 횟수")
                .register(meterRegistry)
                .increment();
    }
}
