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

    /**
     * 결제 성공 후처리
     *
     * <p><strong>⚠️ 중요: 포인트와 캐시백 적립 기준 금액 정책 불일치</strong>
     *
     * <p><strong>현재 구현:</strong>
     * - 포인트: actualPaymentAmount (포인트 차감 후 실제 결제 금액) 기준
     * - 캐시백: originalAmount (포인트 차감 전 원래 금액) 기준
     *
     * <p><strong>예시:</strong> 1000원 주문 - 100원 포인트 사용 = 900원 실제 결제
     * - 현재 포인트 적립: 900원 × 1% = 9포인트
     * - 현재 캐시백 적립: 1000원 × 1% = 10포인트 (캐시백)
     *
     * <p><strong>문제점:</strong>
     * 1. 포인트로 포인트를 적립하는 구조 (포인트 사용분만큼 적립 손실)
     * 2. 캐시백은 포인트 사용분도 포함하여 과다 지급 가능성
     * 3. 두 정책이 서로 모순됨
     *
     * <p><strong>일반적인 업계 관례 (권장):</strong>
     * - 포인트 적립: originalAmount 기준 (포인트 사용과 무관하게 주문 금액 기준)
     * - 캐시백 적립: actualPaymentAmount 기준 (실제 결제 금액 기준)
     * - 또는 포인트 사용 시 포인트/캐시백 적립을 아예 제외
     *
     * <p><strong>비즈니스 정책 결정 필요:</strong>
     * - 포인트 사용 금액도 적립 대상에 포함할 것인가?
     * - 포인트와 캐시백의 적립 기준을 통일할 것인가?
     * - PointPolicy, CashbackPolicy 인터페이스로 추상화하여 유연하게 관리 권장
     *
     * @param payment 결제 정보
     * @param userId 사용자 ID
     * @param actualPaymentAmount 실제 결제 금액 (포인트 차감 후)
     * @param originalAmount 원래 주문 금액 (포인트 차감 전)
     * @param processingTime 처리 시간 (ms)
     */
    public void processPaymentSuccess(Payment payment, Long userId, BigDecimal actualPaymentAmount,
                                     BigDecimal originalAmount, long processingTime) {
        // 로깅
        loggingService.logPaymentApprovalSuccess(payment, payment.getExternalPaymentId(), processingTime);
        loggingService.logPaymentStatusChange(payment, PaymentStatus.PENDING, PaymentStatus.SUCCESS);

        // TODO: 비즈니스 정책 결정 후 수정 필요
        // 권장: originalAmount 기준으로 포인트 적립 (포인트 사용과 무관)
        // 포인트 적립 (현재: 실제 결제 금액 기준 - 정책 검토 필요)
        pointService.accumulatePoints(userId, payment.getId(), actualPaymentAmount);

        // TODO: 비즈니스 정책 결정 후 수정 필요
        // 권장: actualPaymentAmount 기준으로 캐시백 적립 (실제 결제 금액 기준)
        // 캐시백 적립 (현재: 원래 주문 금액 기준 - 정책 검토 필요)
        cashbackService.accumulateCashback(userId, payment.getId(), originalAmount);

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

