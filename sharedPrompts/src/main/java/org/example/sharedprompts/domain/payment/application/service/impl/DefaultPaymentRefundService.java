package org.example.sharedprompts.domain.payment.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.port.in.command.RefundPaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentRefundResult;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.PaymentRefundUseCase;
import org.example.sharedprompts.domain.payment.application.port.out.event.PaymentEventPublisherPort;
import org.example.sharedprompts.domain.payment.application.port.out.paymentgateway.PaymentGatewayPort;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentCommandRepositoryPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.event.PaymentRefundedEvent;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentNotFoundException;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentValidationException;
import org.example.sharedprompts.domain.payment.domain.policy.PaymentStatusTransitionPolicy;
import org.example.sharedprompts.domain.payment.infrastructure.transaction.PaymentTransactionManager;

import java.math.BigDecimal;

/**
 * 결제 환불 유스케이스 구현
 *
 * <p>외부 PG 호출 시 DB 락을 유지하지 않도록 2단계 트랜잭션으로 처리합니다.
 * <ul>
 *   <li>1단계: 락 획득 → 검증 → REFUND_IN_PROGRESS 저장 → 커밋 (락 해제)</li>
 *   <li>2단계: 외부 PG 환불 호출 (락 없음)</li>
 *   <li>3단계: 락 획득 → 환불 반영 → 커밋 → 이벤트 발행</li>
 * </ul>
 * PG 실패 시 1단계에서 설정한 REFUND_IN_PROGRESS를 보상 트랜잭션으로 되돌립니다.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultPaymentRefundService implements PaymentRefundUseCase {

    private final PaymentCommandRepositoryPort paymentRepository;
    private final PaymentGatewayPort paymentGateway;
    private final PaymentEventPublisherPort eventPublisher;
    private final PaymentTransactionManager transactionManager;

    @Override
    public PaymentRefundResult refund(RefundPaymentCommand command) {
        log.info("결제 환불 시작: paymentId={}, userId={}, refundAmount={}",
                command.getPaymentId(), command.getUserId(), command.getRefundAmount());

        // 1단계: 락 하에 검증 후 REFUND_IN_PROGRESS로 저장하고 커밋(락 해제)
        RefundPreparation preparation = transactionManager.executeInTransaction(() ->
                prepareRefund(command));

        BigDecimal refundAmount = preparation.refundAmount();
        Payment paymentAfterPrepare = preparation.payment();

        // 2단계: 외부 PG 환불 호출 (DB 락 없음)
        PaymentGatewayPort.PaymentGatewayResult gatewayResult;
        try {
            gatewayResult = paymentGateway.refundPayment(paymentAfterPrepare, refundAmount);
        } catch (Exception e) {
            revertRefundInProgress(command.getPaymentId());
            throw new PaymentValidationException(
                    "결제 환불 중 오류가 발생했습니다: " + e.getMessage(),
                    e
            );
        }
        if (!gatewayResult.isSuccess()) {
            revertRefundInProgress(command.getPaymentId());
            throw new PaymentValidationException(
                    "결제 환불 중 오류가 발생했습니다: " + gatewayResult.getMessage(),
                    gatewayResult.getException()
            );
        }

        // 3단계: 락 하에 환불 반영 후 커밋
        Payment payment = transactionManager.executeInTransaction(() ->
                completeRefund(command.getPaymentId(), refundAmount));

        PaymentRefundedEvent event = PaymentRefundedEvent.of(
                payment.getId(),
                payment.getUser().getId(),
                refundAmount,
                command.getReason()
        );
        log.info("결제 환불 완료: paymentId={}, refundAmount={}",
                payment.getId(), refundAmount);
        eventPublisher.publishPaymentRefunded(event);

        return PaymentRefundResult.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .refundAmount(refundAmount)
                .reason(command.getReason())
                .build();
    }

    /**
     * 1단계: 락 획득 → 검증 → REFUND_IN_PROGRESS 저장. 트랜잭션 커밋 시 락 해제.
     */
    private RefundPreparation prepareRefund(RefundPaymentCommand command) {
        Payment payment = paymentRepository.findByIdForUpdate(command.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "결제를 찾을 수 없습니다. paymentId=" + command.getPaymentId()));

        if (payment.getUser() == null || !payment.getUser().getId().equals(command.getUserId())) {
            throw new PaymentValidationException(
                    "결제를 환불할 권한이 없습니다. paymentId=" + command.getPaymentId());
        }
        PaymentStatusTransitionPolicy.validateCanRefund(payment);

        BigDecimal refundAmount = command.getRefundAmount();
        BigDecimal refundableAmount = PaymentStatusTransitionPolicy.calculateRefundableAmount(payment);
        if (refundAmount == null) {
            refundAmount = refundableAmount;
        }
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0
                || refundAmount.compareTo(refundableAmount) > 0) {
            throw new PaymentValidationException(
                    "유효하지 않은 환불 금액입니다. 환불 가능: " + refundableAmount + ", 요청: " + refundAmount);
        }

        payment.markRefundInProgress();
        payment = paymentRepository.save(payment);
        return new RefundPreparation(payment, refundAmount);
    }

    /**
     * 3단계: 락 획득 → REFUND_IN_PROGRESS 상태에서 환불 반영 후 저장.
     */
    private Payment completeRefund(Long paymentId, BigDecimal refundAmount) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "결제를 찾을 수 없습니다. paymentId=" + paymentId));
        if (payment.getStatus() != PaymentStatus.REFUND_IN_PROGRESS) {
            throw new PaymentValidationException(
                    "환불 진행 중 상태가 아닙니다. paymentId=" + paymentId + ", status=" + payment.getStatus());
        }
        payment.refund(refundAmount);
        return paymentRepository.save(payment);
    }

    private record RefundPreparation(Payment payment, BigDecimal refundAmount) {}

    /**
     * REFUND_IN_PROGRESS 상태를 보상 트랜잭션으로 되돌립니다.
     * PG 호출 실패 또는 예외 시 호출하여 상태 고착을 방지합니다.
     */
    private void revertRefundInProgress(Long paymentId) {
        transactionManager.executeInTransaction(() -> {
            Payment p = paymentRepository.findByIdForUpdate(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException(
                            "결제를 찾을 수 없습니다. paymentId=" + paymentId));
            p.revertRefundInProgress();
            paymentRepository.save(p);
            return null;
        });
    }
}
