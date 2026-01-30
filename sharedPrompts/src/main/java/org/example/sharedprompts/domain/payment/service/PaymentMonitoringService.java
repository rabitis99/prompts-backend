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
     * 결제 실패 이벤트 리스너
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
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
     * 결제 성공 이벤트 리스너
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
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
     * 일일 결제 실패 통계 조회
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
     * 실패율이 임계값을 초과하는지 확인
     */
    public boolean isFailureRateExceeded(double threshold) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        long totalPayments = paymentRepository.countByDateAndStatus(
                startOfDay,
                PaymentStatus.SUCCESS
        ) + paymentRepository.countByDateAndStatus(
                startOfDay,
                PaymentStatus.FAILED
        );

        if (totalPayments == 0) {
            return false;
        }

        long failedPayments = paymentRepository.countByDateAndStatus(
                startOfDay,
                PaymentStatus.FAILED
        );

        double failureRate = (double) failedPayments / totalPayments;
        return failureRate > threshold;
    }
}

