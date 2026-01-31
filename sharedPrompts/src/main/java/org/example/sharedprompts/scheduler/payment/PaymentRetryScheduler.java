package org.example.sharedprompts.scheduler.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.config.RetryProperties;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.payment.provider.PaymentProviderService;
import org.example.sharedprompts.domain.payment.service.payment.provider.PaymentProviderServiceFactory;
import org.example.sharedprompts.domain.payment.service.payment.PaymentRetryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 재시도 스케줄러
 * - 블로킹/외부 호출 처리
 * - 결제 상태 업데이트는 PaymentRetryService로 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRetryScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentProviderServiceFactory providerServiceFactory;
    private final RetryProperties retryProperties;
    private final PaymentRetryService retryService;

    private static final long SCHEDULE_DELAY_MS = 5 * 60 * 1000L; // 5분

    /**
     * 결제 재시도 스케줄러
     * - 1~5분 단위로 반복 실행
     * - 블로킹 없음 (Thread.sleep 제거)
     * - nextRetryAt 기반으로 재시도 가능한 결제만 조회
     */
    @Scheduled(fixedDelay = SCHEDULE_DELAY_MS)
    @SchedulerLock(
            name = "PaymentRetryScheduler",
            lockAtMostFor = "10m",
            lockAtLeastFor = "1m"
    )
    @LockProviderToUse("fallbackLockProvider")
    public void retryFailedPayments() {
        log.info("PaymentRetryScheduler started");

        // nextRetryAt 기반으로 재시도 가능한 결제 조회
        List<Payment> pendingPayments = paymentRepository.findRetryablePayments(
                retryProperties.getMaxAttempts(),
                LocalDateTime.now()
        );

        if (pendingPayments.isEmpty()) {
            log.debug("재시도할 결제가 없습니다.");
            return;
        }

        log.info("재시도할 결제 수: {}", pendingPayments.size());

        int successCount = 0;
        int failureCount = 0;

        for (Payment payment : pendingPayments) {
            try {
                processPaymentRetry(payment);
                successCount++;
            } catch (Exception e) {
                log.error("결제 재시도 실패: paymentId={}, retryCount={}, error={}",
                        payment.getId(), payment.getRetryCount(), e.getMessage(), e);
                failureCount++;
            }
        }

        log.info("PaymentRetryScheduler finished: success={}, failure={}", successCount, failureCount);
    }

    /**
     * 개별 결제 재시도 처리
     * - 외부 API 호출 (블로킹 없음)
     * - 상태 업데이트는 PaymentRetryService로 위임 (REQUIRES_NEW 트랜잭션)
     * - 실패 시 개별 결제만 처리, 전체 배치 영향 없음
     */
    private void processPaymentRetry(Payment payment) {
        try {
            // 1️⃣ 외부 결제 승인 호출 (블로킹 없음)
            PaymentProviderService providerService = providerServiceFactory.getService(payment.getPaymentMethod());
            String externalPaymentId = providerService.approvePayment(payment);

            // 2️⃣ 상태 업데이트를 별도 서비스로 위임 (REQUIRES_NEW 트랜잭션)
            retryService.updatePaymentStatusSuccess(payment.getId(), externalPaymentId);

            log.info("결제 재시도 성공: paymentId={}, retryCount={}", payment.getId(), payment.getRetryCount());

        } catch (Exception e) {
            // 실패 처리 - 지수 백오프 적용하여 다음 재시도 시간 예약
            retryService.updatePaymentStatusFailure(
                    payment.getId(), 
                    e.getMessage(), 
                    retryProperties.getDelayMs()
            );
            throw e;
        }
    }
}
