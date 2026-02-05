package org.example.sharedprompts.scheduler.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.properties.PaymentExpirationProperties;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.enums.PointType;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 만료 스케줄러
 *
 * <p>PENDING 상태의 결제가 일정 시간(기본 30분) 경과하면 자동으로 만료 처리하고
 * 사용한 포인트를 자동으로 복구합니다.
 *
 * <p><strong>처리 흐름:</strong>
 * <ol>
 *   <li>만료된 PENDING 결제 조회 (createdAt + expirationMinutes 이전)</li>
 *   <li>각 결제에 대해 포인트 복구 수행</li>
 *   <li>결제 상태를 FAILED로 변경 (만료 사유 기록)</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpirationScheduler {

    private final PaymentRepository paymentRepository;
    private final PointService pointService;
    private final PaymentExpirationProperties expirationProperties;
    private final PaymentLoggingService loggingService;

    private static final long SCHEDULE_DELAY_MS = 5 * 60 * 1000L; // 5분

    /**
     * 결제 만료 처리 스케줄러
     * - 5분마다 실행
     * - 만료된 PENDING 결제를 조회하여 포인트 복구 및 상태 변경
     */
    @Scheduled(fixedDelay = SCHEDULE_DELAY_MS)
    @SchedulerLock(
            name = "PaymentExpirationScheduler",
            lockAtMostFor = "10m",
            lockAtLeastFor = "1m"
    )
    @LockProviderToUse("fallbackLockProvider")
    public void expirePendingPayments() {
        log.info("PaymentExpirationScheduler started");

        // 만료 기준 시간 계산 (현재 시간 - 만료 시간)
        LocalDateTime expirationTime = LocalDateTime.now()
                .minus(Duration.ofMinutes(expirationProperties.getExpirationMinutes()));

        // 만료된 PENDING 결제 조회 (복구되지 않은 포인트가 있는 결제만)
        List<Payment> expiredPayments = paymentRepository.findExpiredPendingPayments(
                PaymentStatus.PENDING,
                expirationTime
        );

        if (expiredPayments.isEmpty()) {
            log.debug("만료된 결제가 없습니다.");
            return;
        }

        log.info("만료된 결제 수: {}", expiredPayments.size());

        int successCount = 0;
        int failureCount = 0;

        for (Payment payment : expiredPayments) {
            try {
                processPaymentExpiration(payment);
                successCount++;
            } catch (Exception e) {
                log.error("결제 만료 처리 실패: paymentId={}, userId={}, error={}",
                        payment.getId(), payment.getUser().getId(), e.getMessage(), e);
                failureCount++;
            }
        }

        log.info("PaymentExpirationScheduler finished: success={}, failure={}", successCount, failureCount);
    }

    /**
     * 개별 결제 만료 처리
     *
     * <p>포인트 복구 → 결제 상태 FAILED 변경 순서로 처리합니다.
     * 포인트 복구 실패 시에도 결제 상태는 변경하여 중복 처리 방지합니다.
     */
    @Transactional
    public void processPaymentExpiration(Payment payment) {
        Long userId = payment.getUser().getId();
        Long paymentId = payment.getId();

        log.info("결제 만료 처리 시작: paymentId={}, userId={}, usedPointAmount={}, createdAt={}",
                paymentId, userId, payment.getUsedPointAmount(), payment.getCreatedAt());

        // 포인트 복구 처리
        if (payment.hasUnrecoveredPoints()) {
            try {
                pointService.addPointsDirectly(
                        userId,
                        paymentId,
                        payment.getUsedPointAmount(),
                        PointType.PAYMENT_FAILED,
                        "결제 만료로 인한 포인트 복구"
                );
                log.info("결제 만료로 인한 포인트 복구 성공: userId={}, paymentId={}, refundPointAmount={}",
                        userId, paymentId, payment.getUsedPointAmount());
            } catch (Exception pointException) {
                // 포인트 복구 실패 시 로깅하고 계속 진행 (결제 상태는 변경하여 중복 처리 방지)
                log.error("결제 만료 후 포인트 복구 실패: userId={}, paymentId={}, amount={}, error={}",
                        userId, paymentId, payment.getUsedPointAmount(), pointException.getMessage(), pointException);
            }
        }

        // 결제 상태를 FAILED로 변경 (만료 사유 기록)
        String expirationReason = String.format("결제 만료 (요청 후 %d분 경과)", expirationProperties.getExpirationMinutes());
        payment.markFailed(expirationReason);
        paymentRepository.save(payment);

        // 로깅
        loggingService.logPaymentStatusChange(payment, PaymentStatus.PENDING, PaymentStatus.FAILED);
        loggingService.logPaymentApprovalFailure(payment, expirationReason, null);

        log.info("결제 만료 처리 완료: paymentId={}, userId={}, status={}",
                paymentId, userId, payment.getStatus());
    }
}