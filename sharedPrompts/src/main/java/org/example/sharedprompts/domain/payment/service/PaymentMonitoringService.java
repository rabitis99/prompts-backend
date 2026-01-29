package org.example.sharedprompts.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.metrics.PaymentMetrics;
import org.example.sharedprompts.domain.payment.repository.PaymentRepository;
import org.example.sharedprompts.domain.payment.statistics.PaymentFailureStatistics;
import org.example.sharedprompts.domain.payment.event.PaymentEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 결제 모니터링 서비스
 * 결제 실패 모니터링 및 알림 기능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMonitoringService {

    private final PaymentRepository paymentRepository;
    private final PaymentNotificationService notificationService;
    private final PaymentMetrics paymentMetrics;

    /**
     * Handle a payment failure event by sending a failure notification and recording failure metrics.
     *
     * @param event the payment failure event containing paymentId, userId, failure reason, and paymentMethod
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @EventListener
    public void handlePaymentFailed(PaymentEvent.PaymentFailed event) {
        log.warn("결제 실패 감지: paymentId={}, userId={}, reason={}, paymentMethod={}",
                event.paymentId(), event.userId(), event.reason(), event.paymentMethod());

        // 알림 발송
        notificationService.sendPaymentFailureNotification(
                event.paymentId(),
                event.userId(),
                event.reason(),
                event.paymentMethod()
        );

        // 모니터링 메트릭 업데이트
        paymentMetrics.recordPaymentFailure(event.paymentMethod(), event.reason(), 0);
    }

    /**
     * Handles a payment success event by logging the event, sending a success notification if the payment record exists, and recording a success metric.
     *
     * <p>The method locates the Payment entity by the event's paymentId to include the payment amount in the notification when available.</p>
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @EventListener
    public void handlePaymentSucceeded(PaymentEvent.PaymentSucceeded event) {
        log.info("결제 성공: paymentId={}, userId={}, paymentMethod={}",
                event.paymentId(), event.userId(), event.paymentMethod());

        // 알림 발송
        Optional<Payment> paymentOpt = paymentRepository.findById(event.paymentId());
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            notificationService.sendPaymentSuccessNotification(
                    event.paymentId(),
                    event.userId(),
                    event.paymentMethod(),
                    payment.getAmount().toString()
            );
        }

        // 성공 메트릭 업데이트
        paymentMetrics.recordPaymentSuccess(event.paymentMethod(), 0);
    }

    /**
     * Compute daily payment failure statistics for the current date.
     *
     * Builds statistics from payments with status FAILED that were created after the start of today,
     * aggregating the total number of failures and counts per payment method.
     *
     * @return a PaymentFailureStatistics for the current date containing the total failures and a map of failures by payment method
     */
    public PaymentFailureStatistics getDailyFailureStatistics() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        List<Payment> failedPayments = paymentRepository.findByStatusOrderByCreatedAtDesc(PaymentStatus.FAILED)
                .stream()
                .filter(p -> p.getCreatedAt().isAfter(startOfDay))
                .collect(Collectors.toList());

        long totalFailures = failedPayments.size();
        Map<String, Long> failuresByMethod = failedPayments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPaymentMethod().name(),
                        Collectors.counting()
                ));

        return PaymentFailureStatistics.builder()
                .date(LocalDateTime.now().toLocalDate())
                .totalFailures(totalFailures)
                .failuresByMethod(failuresByMethod)
                .build();
    }

    /**
     * Determines whether today's payment failure rate exceeds the given threshold.
     *
     * Calculates the failure rate for all users from the start of the current day (midnight).
     * If there are no payments today, the method returns `false`.
     *
     * @param threshold the failure-rate threshold as a fraction (e.g., 0.05 for 5%)
     * @return `true` if the computed failure rate is greater than `threshold`, `false` otherwise
     */
    public boolean isFailureRateExceeded(double threshold) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        long totalPayments = paymentRepository.countByUserIdAndDateAndStatus(
                null, // 전체 사용자
                startOfDay,
                PaymentStatus.SUCCESS
        ) + paymentRepository.countByUserIdAndDateAndStatus(
                null,
                startOfDay,
                PaymentStatus.FAILED
        );

        if (totalPayments == 0) {
            return false;
        }

        long failedPayments = paymentRepository.countByUserIdAndDateAndStatus(
                null,
                startOfDay,
                PaymentStatus.FAILED
        );

        double failureRate = (double) failedPayments / totalPayments;
        return failureRate > threshold;
    }
}
