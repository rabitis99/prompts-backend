package org.example.sharedprompts.scheduler.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.config.RetryProperties;
import org.example.sharedprompts.domain.payment.facade.PaymentRetryFacade;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.execution.PaymentExecutionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 재시도 스케줄러
 * - 블로킹/외부 호출 처리
 * - PaymentRetryFacade를 통한 재시도 상태 관리로 PaymentRetryFacade와 일관성 유지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRetryScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentExecutionService executionService;
    private final RetryProperties retryProperties;
    private final PaymentRetryFacade retryFacade;
    private final PaymentLoggingService loggingService;

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
     *
     * <p>트랜잭션 전략 (PaymentRetryFacade와 일관성 유지):
     * 1. 재시도 상태 확정(횟수 증가, 다음 재시도 시간, 상태 변경)은 별도 트랜잭션(REQUIRES_NEW)에서 먼저 커밋
     * 2. 결제 실행은 메인 트랜잭션에서 수행하며, 실패 시 예외 전파
     *
     * <p>이를 통해 executePayment() 실패 여부와 관계없이
     * 재시도 관련 상태 변경(retryCount, nextRetryAt, status)은 반드시 DB에 반영됩니다.
     * 또한 PaymentExecutionService.executePayment() 내부의 retryCount > 0 체크가 올바르게 작동합니다.
     */
    private void processPaymentRetry(Payment payment) {
        try {
            // 재시도 로깅
            loggingService.logRetryAttempt(payment, payment.getRetryCount() + 1);

            // 재시도 상태 확정: 별도 트랜잭션에서 먼저 커밋
            // executePayment() 실패 여부와 관계없이 재시도 상태는 반드시 DB에 반영됨
            // 이 시점에서 retryCount가 증가하므로, executePayment() 내부의 retryCount > 0 체크가 올바르게 작동
            Payment updatedPayment = retryFacade.commitRetryState(payment);

            // 실제 결제 금액 계산 (포인트 사용 후 금액)
            BigDecimal actualAmount = updatedPayment.getAmount().subtract(
                    updatedPayment.getUsedPointAmount() != null ? updatedPayment.getUsedPointAmount() : BigDecimal.ZERO
            );

            // PaymentExecutionService를 통한 재시도 실행
            // 실패 시 예외가 상위로 전파되지만, 재시도 상태는 이미 커밋됨
            Payment retriedPayment = executionService.executePayment(updatedPayment, actualAmount);

            log.info("결제 재시도 성공: paymentId={}, retryCount={}, status={}",
                    retriedPayment.getId(), retriedPayment.getRetryCount(), retriedPayment.getStatus());

        } catch (Exception e) {
            // 재시도 상태는 이미 commitRetryState()에서 커밋되었으므로
            // 여기서는 로깅만 수행 (updatePaymentStatusFailure 호출 불필요 - 중복 증가 방지)
            log.error("결제 재시도 실행 실패: paymentId={}, retryCount={}, error={}",
                    payment.getId(), payment.getRetryCount(), e.getMessage(), e);
            throw e;
        }
    }
}
