package org.example.sharedprompts.domain.payment.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.port.in.command.CancelPaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentCancellationResult;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.PaymentCancellationUseCase;
import org.example.sharedprompts.domain.payment.application.port.out.event.PaymentEventPublisherPort;
import org.example.sharedprompts.domain.payment.application.port.out.paymentgateway.PaymentGatewayPort;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentCommandRepositoryPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.event.PaymentCanceledEvent;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentNotFoundException;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentValidationException;
import org.example.sharedprompts.domain.payment.domain.policy.PaymentStatusTransitionPolicy;
import org.example.sharedprompts.domain.payment.infrastructure.transaction.PaymentTransactionManager;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 결제 취소 유스케이스 구현
 * 락 점유 시간을 줄이기 위해 검증(1단계) → PG 호출(락 밖) → 상태 반영(2단계)으로 분리합니다.
 *
 * <p><b>트랜잭션·동시성 안전성</b>:
 * <ul>
 *   <li>각 단계는 {@link PaymentTransactionManager#executeInTransaction}으로 별도 트랜잭션(REQUIRES_NEW)에서 실행되며,
 *       락은 1단계 커밋 시 해제되므로 PG 호출 중에는 DB 락을 점유하지 않는다.</li>
 *   <li>3단계 진입 시 {@code findByIdForUpdate}로 최신 상태를 조회한 뒤,
 *       {@link PaymentStatusTransitionPolicy#validateCanCancel}로 재검증하여, 1단계 이후 다른 프로세스가
 *       환불/확인 등으로 상태를 변경한 경우 예외를 던지고 덮어쓰기를 방지한다.</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultPaymentCancellationService implements PaymentCancellationUseCase {

    private final PaymentCommandRepositoryPort paymentRepository;
    private final PaymentGatewayPort paymentGateway;
    private final PaymentEventPublisherPort eventPublisher;
    private final PaymentTransactionManager transactionManager;

    @Override
    public PaymentCancellationResult cancel(CancelPaymentCommand command) {
        log.info("결제 취소 시작: paymentId={}, userId={}, reason={}",
                command.getPaymentId(), command.getUserId(), command.getReason());

        // 1단계: 짧은 트랜잭션에서 락으로 검증 및 취소 금액 계산 후 락 해제
        CancellationPreparation preparation = transactionManager.executeInTransaction(() ->
                prepareCancellation(command));

        // 2단계: 락 밖에서 외부 PG 취소 호출 (네트워크 지연 시에도 DB 락 점유 없음)
        if (preparation.payment.getStatus() == PaymentStatus.SUCCESS) {
            PaymentGatewayPort.PaymentGatewayResult gatewayResult = paymentGateway.cancelPayment(preparation.payment);
            if (!gatewayResult.isSuccess()) {
                throw new PaymentValidationException(
                        "결제 취소 중 오류가 발생했습니다: " + gatewayResult.getMessage(),
                        gatewayResult.getException()
                );
            }
        }

        // 3단계: 별도 트랜잭션에서 상태 업데이트 및 이벤트 발행(커밋 후)
        return transactionManager.executeInTransaction(() ->
                applyCancellation(command.getPaymentId(), command.getUserId(), command.getReason(), preparation.cancelAmount));
    }

    /**
     * 1단계: 락을 잡고 검증·취소 금액만 계산 후 반환. 트랜잭션 커밋 시 락 해제.
     */
    private CancellationPreparation prepareCancellation(CancelPaymentCommand command) {
        Payment payment = paymentRepository.findByIdForUpdate(command.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "결제를 찾을 수 없습니다. paymentId=" + command.getPaymentId()
                ));

        if (payment.getUser() == null || !Objects.equals(payment.getUser().getId(), command.getUserId())) {
            throw new PaymentValidationException(
                    "결제를 취소할 권한이 없습니다. paymentId=" + command.getPaymentId()
            );
        }

        PaymentStatusTransitionPolicy.validateCanCancel(payment);
        BigDecimal cancelAmount = PaymentStatusTransitionPolicy.calculateRefundableAmount(payment);
        return new CancellationPreparation(payment, cancelAmount);
    }

    /**
     * 3단계: PG 취소 완료 후 상태 반영 및 이벤트 발행(어댑터 afterCommit에서 발행).
     * 1단계 커밋 후 다른 트랜잭션이 결제를 변경했을 수 있으므로, 반영 전 취소 가능 상태를 재검증한다.
     */
    private PaymentCancellationResult applyCancellation(Long paymentId, Long userId, String reason, BigDecimal cancelAmount) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "결제를 찾을 수 없습니다. paymentId=" + paymentId
                ));

        // 1단계 커밋 후 다른 프로세스가 환불/확인 등으로 상태를 바꿨을 수 있음 → 재검증
        PaymentStatusTransitionPolicy.validateCanCancel(payment);

        payment.markCanceled();
        payment = paymentRepository.save(payment);

        PaymentCanceledEvent event = PaymentCanceledEvent.of(
                payment.getId(),
                payment.getUser().getId(),
                cancelAmount,
                reason
        );
        eventPublisher.publishPaymentCanceled(event);

        log.info("결제 취소 완료: paymentId={}, cancelAmount={}", payment.getId(), cancelAmount);

        return PaymentCancellationResult.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .refundedAmount(cancelAmount)
                .reason(reason)
                .build();
    }

    /** 1단계 검증 결과 (PG 호출에 필요한 정보만 전달) */
    private record CancellationPreparation(Payment payment, BigDecimal cancelAmount) {}
}
