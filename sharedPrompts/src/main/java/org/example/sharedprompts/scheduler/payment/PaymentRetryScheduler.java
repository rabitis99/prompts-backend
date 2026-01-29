package org.example.sharedprompts.scheduler.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.example.sharedprompts.domain.payment.repository.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.payment.provider.PaymentProviderServiceFactory;
import org.example.sharedprompts.domain.payment.service.payment.provider.PaymentProviderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 결제 재시도 스케줄러
 * 실패한 결제에 대한 자동 재시도 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRetryScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentProviderServiceFactory providerServiceFactory;
    private final PaymentProperties paymentProperties;

    private static final long SCHEDULE_DELAY_MS = 5 * 60 * 1000L; // 5분마다 실행

    @Scheduled(fixedDelay = SCHEDULE_DELAY_MS)
    @SchedulerLock(
            name = "PaymentRetryScheduler",
            lockAtMostFor = "10m",
            lockAtLeastFor = "1m"
    )
    @LockProviderToUse("fallbackLockProvider")
    @Transactional
    public void retryFailedPayments() {
        log.info("PaymentRetryScheduler started");

        try {
            // 재시도가 필요한 결제 목록 조회 (PENDING 상태이고 재시도 횟수가 제한 미만)
            List<Payment> pendingPayments = paymentRepository.findPendingPaymentsForRetry(
                    paymentProperties.getMaxRetryAttempts()
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
                    retryPayment(payment);
                    successCount++;
                } catch (Exception e) {
                    log.error("결제 재시도 실패: paymentId={}, retryCount={}, error={}",
                            payment.getId(), payment.getRetryCount(), e.getMessage(), e);
                    failureCount++;
                    
                    // 재시도 횟수 증가
                    payment.incrementRetryCount();
                    
                    // 최대 재시도 횟수 초과 시 실패 처리
                    if (payment.getRetryCount() >= paymentProperties.getMaxRetryAttempts()) {
                        payment.fail("최대 재시도 횟수 초과");
                        log.warn("결제 최대 재시도 횟수 초과로 실패 처리: paymentId={}", payment.getId());
                    }
                    
                    paymentRepository.save(payment);
                }
            }

            log.info("PaymentRetryScheduler finished: success={}, failure={}", successCount, failureCount);
        } catch (Exception e) {
            log.error("PaymentRetryScheduler 실행 중 오류 발생", e);
        }
    }

    private void retryPayment(Payment payment) {
        try {
            PaymentProviderService providerService = providerServiceFactory.getService(payment.getPaymentMethod());
            
            // 재시도 전 대기 (지수 백오프)
            long delayMs = paymentProperties.getRetryDelayMs() * (long) Math.pow(2, payment.getRetryCount());
            Thread.sleep(Math.min(delayMs, 60000)); // 최대 60초

            // 결제 승인 재시도
            String externalPaymentId = providerService.approvePayment(payment);
            payment.approve(externalPaymentId);
            paymentRepository.save(payment);

            log.info("결제 재시도 성공: paymentId={}, retryCount={}", payment.getId(), payment.getRetryCount());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("재시도 대기 중 인터럽트 발생", e);
        } catch (Exception e) {
            log.error("결제 재시도 중 오류 발생: paymentId={}, error={}", payment.getId(), e.getMessage(), e);
            throw e;
        }
    }
}

