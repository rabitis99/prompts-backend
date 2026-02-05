package org.example.sharedprompts.domain.payment.application.command.postprocess;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.PointType;
import org.example.sharedprompts.domain.payment.domain.valueobject.PaymentAmount;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentMetrics;
import org.example.sharedprompts.domain.payment.service.cashback.CashbackService;
import org.example.sharedprompts.domain.payment.infrastructure.messaging.event.PaymentEventPublisher;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.application.command.postprocess.policy.CashbackAccrualPolicy;
import org.example.sharedprompts.domain.payment.application.command.postprocess.policy.PointAccrualPolicy;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
    private final PaymentJpaAdapter paymentJpaAdapter;

    public void processPaymentSuccess(Payment payment, Long userId, BigDecimal actualPaymentAmount,
                                     BigDecimal originalAmount, long processingTime) {
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            log.warn("결제 성공 후처리 스킵: 결제 상태가 SUCCESS가 아님. paymentId={}, status={}", 
                    payment.getId(), payment.getStatus());
            return;
        }

        loggingService.logPaymentApprovalSuccess(payment, payment.getExternalPaymentId(), processingTime);
        PaymentStatus oldStatus = payment.getStatus() != PaymentStatus.SUCCESS ? PaymentStatus.PENDING : payment.getStatus();
        loggingService.logPaymentStatusChange(payment, oldStatus, PaymentStatus.SUCCESS);

        executeSuccessPostProcessing(payment, userId, actualPaymentAmount, originalAmount, processingTime);
    }

    public void processPaymentFailure(Payment payment, Long userId, String errorMessage,
                                     Exception exception, long processingTime) {
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
                log.error("결제 실패 후 포인트 복구 실패: userId={}, paymentId={}, amount={}, error={}",
                        userId, payment.getId(), payment.getUsedPointAmount(), pointException.getMessage(), pointException);
            }
        }

        loggingService.logPaymentApprovalFailure(payment, errorMessage, exception);
        paymentMetrics.recordPaymentFailure(payment.getPaymentMethod().name(), errorMessage, processingTime);
        eventPublisher.publishPaymentFailed(
                payment.getId(),
                userId,
                errorMessage,
                payment.getPaymentMethod().name()
        );
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processPaymentCancelAfterCommit(Long paymentId, Long userId, String reason) {
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        PaymentStatus oldStatus = payment.getStatus() != PaymentStatus.CANCELED 
                ? PaymentStatus.SUCCESS 
                : PaymentStatus.CANCELED;
        
        processPaymentCancel(payment, userId, reason, oldStatus);
    }

    public void processPaymentCancel(Payment payment, Long userId, String reason, PaymentStatus oldStatus) {
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
                log.error("결제 취소 후 포인트 복구 실패: userId={}, paymentId={}, amount={}, error={}",
                        userId, payment.getId(), payment.getUsedPointAmount(), pointException.getMessage(), pointException);
            }
        }

        loggingService.logPaymentCancel(payment, reason);
        loggingService.logPaymentStatusChange(payment, oldStatus, PaymentStatus.CANCELED);
        paymentMetrics.recordPaymentCancel(payment.getPaymentMethod().name());
        eventPublisher.publishPaymentCanceled(payment.getId(), userId, reason);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processPaymentRefundAfterCommit(Long paymentId, Long userId, BigDecimal refundAmount,
                                               BigDecimal refundPointAmount, String reason) {
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        PaymentStatus oldStatus = payment.getStatus() != PaymentStatus.REFUNDED 
                ? PaymentStatus.SUCCESS 
                : PaymentStatus.REFUNDED;
        
        processPaymentRefund(payment, userId, refundAmount, refundPointAmount, reason, oldStatus);
    }

    public void processPaymentRefund(Payment payment, Long userId, BigDecimal refundAmount,
                                    BigDecimal refundPointAmount, String reason, PaymentStatus oldStatus) {
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
                log.error("결제 환불 후 포인트 복구 실패: userId={}, paymentId={}, amount={}, error={}",
                        userId, payment.getId(), refundPointAmount, pointException.getMessage(), pointException);
            }
        }

        loggingService.logPaymentRefund(payment, refundAmount, reason);
        loggingService.logPaymentStatusChange(payment, oldStatus, payment.getStatus());
        paymentMetrics.recordPaymentRefund(payment.getPaymentMethod().name(), refundAmount.doubleValue());
        eventPublisher.publishPaymentRefunded(payment.getId(), userId, reason);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processPaymentSuccessAfterCommit(Long paymentId, Long userId, BigDecimal actualPaymentAmount,
                                                 BigDecimal originalAmount, long processingTime) {
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            log.warn("결제 성공 후처리 스킵: 결제 상태가 SUCCESS가 아님. paymentId={}, status={}", 
                    paymentId, payment.getStatus());
            return;
        }

        executeSuccessPostProcessing(payment, userId, actualPaymentAmount, originalAmount, processingTime);
    }

    private void executeSuccessPostProcessing(Payment payment, Long userId, 
            BigDecimal actualPaymentAmount, BigDecimal originalAmount, long processingTime) {
        PaymentAmount originalPaymentAmount = PaymentAmount.of(
            originalAmount,
            payment.getCurrency()
        );
        PaymentAmount actualPaymentAmountVO = PaymentAmount.of(
            actualPaymentAmount,
            payment.getCurrency()
        );
        
        PaymentAmount usedPointAmountVO = originalPaymentAmount.subtract(actualPaymentAmountVO);
        BigDecimal usedPointAmount = usedPointAmountVO.toBigDecimal();
        BigDecimal pointBasisAmount = pointAccrualPolicy.determineBasisAmount(
                originalAmount, actualPaymentAmount, usedPointAmount);
        pointService.accumulatePoints(userId, payment.getId(), pointBasisAmount);
        BigDecimal cashbackBasisAmount = cashbackAccrualPolicy.determineBasisAmount(
                originalAmount, actualPaymentAmount, usedPointAmount);
        cashbackService.accumulateCashback(userId, payment.getId(), cashbackBasisAmount);

        log.debug("리워드 적립 완료: paymentId={}, pointPolicy={}, pointBasis={}, cashbackPolicy={}, cashbackBasis={}",
                payment.getId(),
                pointAccrualPolicy.getPolicyName(), pointBasisAmount,
                cashbackAccrualPolicy.getPolicyName(), cashbackBasisAmount);
        paymentMetrics.recordPaymentSuccess(payment.getPaymentMethod().name(), processingTime);
        paymentMetrics.recordPaymentAmount(payment.getPaymentMethod().name(), originalAmount.doubleValue());
        eventPublisher.publishPaymentSucceeded(payment.getId(), userId, payment.getPaymentMethod().name());
    }
}

