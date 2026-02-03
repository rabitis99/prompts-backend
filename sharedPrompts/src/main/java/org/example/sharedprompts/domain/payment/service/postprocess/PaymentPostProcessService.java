package org.example.sharedprompts.domain.payment.service.postprocess;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.enums.PointType;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.metrics.PaymentMetrics;
import org.example.sharedprompts.domain.payment.service.cashback.CashbackService;
import org.example.sharedprompts.domain.payment.service.event.PaymentEventPublisher;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.example.sharedprompts.domain.payment.service.postprocess.policy.CashbackAccrualPolicy;
import org.example.sharedprompts.domain.payment.service.postprocess.policy.PointAccrualPolicy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 결제 후처리 서비스
 * 
 * <p>단일 책임: 결제 후처리만 담당
 * - 포인트/캐시백 적립
 * - 이벤트 발행
 * - 메트릭 수집
 * - 로깅
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPostProcessService {

    private final PointService pointService;
    private final CashbackService cashbackService;
    private final PaymentEventPublisher eventPublisher;
    private final PaymentMetrics paymentMetrics;
    private final PaymentLoggingService loggingService;
    private final PointAccrualPolicy pointAccrualPolicy;
    private final CashbackAccrualPolicy cashbackAccrualPolicy;

    /**
     * 결제 성공 후처리
     *
     * <p>포인트/캐시백 적립 기준 금액은 각각의 정책 클래스에서 결정됩니다.
     *
     * @param payment 결제 정보
     * @param userId 사용자 ID
     * @param actualPaymentAmount 실제 결제 금액 (포인트 차감 후)
     * @param originalAmount 원래 주문 금액 (포인트 차감 전)
     * @param processingTime 처리 시간 (ms)
     * @see PointAccrualPolicy
     * @see CashbackAccrualPolicy
     */
    public void processPaymentSuccess(Payment payment, Long userId, BigDecimal actualPaymentAmount,
                                     BigDecimal originalAmount, long processingTime) {
        // 로깅
        loggingService.logPaymentApprovalSuccess(payment, payment.getExternalPaymentId(), processingTime);
        loggingService.logPaymentStatusChange(payment, PaymentStatus.PENDING, PaymentStatus.SUCCESS);

        // 사용된 포인트 금액 계산
        BigDecimal usedPointAmount = originalAmount.subtract(actualPaymentAmount);

        // 포인트 적립 (정책에 따른 기준 금액 결정)
        BigDecimal pointBasisAmount = pointAccrualPolicy.determineBasisAmount(
                originalAmount, actualPaymentAmount, usedPointAmount);
        pointService.accumulatePoints(userId, payment.getId(), pointBasisAmount);

        // 캐시백 적립 (정책에 따른 기준 금액 결정)
        BigDecimal cashbackBasisAmount = cashbackAccrualPolicy.determineBasisAmount(
                originalAmount, actualPaymentAmount, usedPointAmount);
        cashbackService.accumulateCashback(userId, payment.getId(), cashbackBasisAmount);

        log.debug("리워드 적립 완료: paymentId={}, pointPolicy={}, pointBasis={}, cashbackPolicy={}, cashbackBasis={}",
                payment.getId(),
                pointAccrualPolicy.getPolicyName(), pointBasisAmount,
                cashbackAccrualPolicy.getPolicyName(), cashbackBasisAmount);

        // 메트릭 기록
        paymentMetrics.recordPaymentSuccess(payment.getPaymentMethod().name(), processingTime);
        paymentMetrics.recordPaymentAmount(payment.getPaymentMethod().name(), originalAmount.doubleValue());

        // 결제 성공 이벤트 발행
        eventPublisher.publishPaymentSucceeded(payment.getId(), userId, payment.getPaymentMethod().name());
    }

    /**
     * 결제 실패 후처리
     *
     * <p>결제 요청(requestPayment) 시점에 포인트가 이미 차감되었으므로,
     * 결제 승인(confirmPayment) 실패 시 포인트를 복구해야 합니다.
     */
    public void processPaymentFailure(Payment payment, Long userId, String errorMessage,
                                     Exception exception, long processingTime) {
        // 포인트 복구 처리 (결제 요청 시 차감된 포인트 복구)
        if (payment.getUsedPointAmount() != null && payment.getUsedPointAmount().compareTo(BigDecimal.ZERO) > 0) {
            try {
                pointService.addPointsDirectly(
                        userId,
                        payment.getId(),
                        payment.getUsedPointAmount(),
                        PointType.PAYMENT_FAILED,
                        "결제 실패로 인한 포인트 복구"
                );
                log.info("결제 실패로 인한 포인트 복구: userId={}, paymentId={}, refundPointAmount={}",
                        userId, payment.getId(), payment.getUsedPointAmount());
            } catch (Exception pointException) {
                // 포인트 복구 실패 시 로깅하고 계속 진행 (별도 보상 처리 필요)
                log.error("결제 실패 후 포인트 복구 실패: userId={}, paymentId={}, amount={}, error={}",
                        userId, payment.getId(), payment.getUsedPointAmount(), pointException.getMessage(), pointException);
            }
        }

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
     * 결제 취소 후처리
     *
     * <p>포인트 환불 시 addPointsDirectly를 사용하여 사용했던 포인트를 그대로 복구합니다.
     * accumulatePoints는 결제 금액에 포인트 적립률을 곱하므로 환불/취소에는 부적합합니다.
     */
    public void processPaymentCancel(Payment payment, Long userId, String reason, PaymentStatus oldStatus) {
        // 포인트 환불 처리 (사용했던 포인트 그대로 복구)
        if (payment.getUsedPointAmount() != null && payment.getUsedPointAmount().compareTo(BigDecimal.ZERO) > 0) {
            try {
                pointService.addPointsDirectly(
                        userId,
                        payment.getId(),
                        payment.getUsedPointAmount(),
                        PointType.CANCEL,
                        "결제 취소로 인한 포인트 복구"
                );
                log.info("결제 취소로 인한 포인트 복구: userId={}, paymentId={}, refundPointAmount={}",
                        userId, payment.getId(), payment.getUsedPointAmount());
            } catch (Exception pointException) {
                // 포인트 복구 실패 시 로깅하고 계속 진행 (별도 보상 처리 필요)
                log.error("결제 취소 후 포인트 복구 실패: userId={}, paymentId={}, amount={}, error={}",
                        userId, payment.getId(), payment.getUsedPointAmount(), pointException.getMessage(), pointException);
            }
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
     * 결제 환불 후처리
     *
     * <p>포인트 환불 시 addPointsDirectly를 사용하여 사용했던 포인트를 그대로 복구합니다.
     * accumulatePoints는 결제 금액에 포인트 적립률을 곱하므로 환불/취소에는 부적합합니다.
     */
    public void processPaymentRefund(Payment payment, Long userId, BigDecimal refundAmount,
                                    BigDecimal refundPointAmount, String reason, PaymentStatus oldStatus) {
        // 포인트 환불 처리 (사용했던 포인트 그대로 복구)
        if (refundPointAmount != null && refundPointAmount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                pointService.addPointsDirectly(
                        userId,
                        payment.getId(),
                        refundPointAmount,
                        PointType.REFUND,
                        "결제 환불로 인한 포인트 복구"
                );
                log.info("결제 환불로 인한 포인트 복구: userId={}, paymentId={}, refundPointAmount={}",
                        userId, payment.getId(), refundPointAmount);
            } catch (Exception pointException) {
                // 포인트 복구 실패 시 로깅하고 계속 진행 (별도 보상 처리 필요)
                log.error("결제 환불 후 포인트 복구 실패: userId={}, paymentId={}, amount={}, error={}",
                        userId, payment.getId(), refundPointAmount, pointException.getMessage(), pointException);
            }
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

