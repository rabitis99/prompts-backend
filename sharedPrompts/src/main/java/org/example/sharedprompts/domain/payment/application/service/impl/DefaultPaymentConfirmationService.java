package org.example.sharedprompts.domain.payment.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.port.in.command.ConfirmPaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentConfirmationResult;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.PaymentConfirmationUseCase;
import org.example.sharedprompts.domain.payment.application.port.out.event.PaymentEventPublisherPort;
import org.example.sharedprompts.domain.payment.application.port.out.paymentgateway.PaymentConfirmParams;
import org.example.sharedprompts.domain.payment.application.port.out.paymentgateway.PaymentGatewayPort;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentCommandRepositoryPort;
import org.example.sharedprompts.domain.payment.application.service.PaymentConfirmParamsResolver;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentErrorCode;
import org.example.sharedprompts.domain.payment.domain.event.PaymentConfirmedEvent;
import org.example.sharedprompts.domain.payment.domain.event.PaymentFailedEvent;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentNotFoundException;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentValidationException;
import org.example.sharedprompts.domain.payment.domain.policy.PaymentStatusTransitionPolicy;
import org.example.sharedprompts.domain.payment.infrastructure.transaction.PaymentTransactionManager;

import java.util.Objects;

/**
 * 결제 확인 유스케이스 구현
 * 외부 결제 제공자로부터 토큰을 받아 최종 승인을 처리합니다.
 * 락 점유 시간을 줄이기 위해 검증(1단계) → PG 호출(락 밖) → 상태 반영(2단계)으로 분리합니다.
 *
 * <p><b>트랜잭션·동시성 안전성</b>:
 * <ul>
 *   <li>각 단계는 {@link PaymentTransactionManager#executeInTransaction}으로 별도 트랜잭션(REQUIRES_NEW)에서 실행되며,
 *       락은 1단계 커밋 시 해제되므로 PG 호출 중에는 DB 락을 점유하지 않는다.</li>
 *   <li>3단계 진입 시 {@code findByIdForUpdate}로 최신 상태를 조회한 뒤,
 *       {@link PaymentStatusTransitionPolicy#validateCanApprove}로 재검증하여, 1단계 이후 다른 프로세스가
 *       취소/환불 등으로 상태를 변경한 경우 예외를 던지고 덮어쓰기를 방지한다.</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultPaymentConfirmationService implements PaymentConfirmationUseCase {

    private final PaymentCommandRepositoryPort paymentRepository;
    private final PaymentGatewayPort paymentGateway;
    private final PaymentConfirmParamsResolver confirmParamsResolver;
    private final PaymentEventPublisherPort eventPublisher;
    private final PaymentTransactionManager transactionManager;

    @Override
    public PaymentConfirmationResult confirm(ConfirmPaymentCommand command) {
        log.info("결제 확인 시작: paymentId={}, userId={}", command.getPaymentId(), command.getUserId());

        // 1단계: 짧은 트랜잭션에서 락으로 검증 후 락 해제
        Payment payment = transactionManager.executeInTransaction(() ->
                prepareConfirmation(command));

        // 2단계: 락 밖에서 외부 PG 승인 호출
        PaymentConfirmParams params = confirmParamsResolver.resolve(payment, command);
        PaymentGatewayPort.PaymentGatewayResult confirmResult = paymentGateway.confirmPayment(payment, params);

        // 3단계: 별도 트랜잭션에서 결과 반영 및 이벤트 발행(커밋 후)
        return transactionManager.executeInTransaction(() ->
                applyConfirmationResult(command, payment.getId(), confirmResult));
    }

    /**
     * 1단계: 락을 잡고 검증만 수행. 트랜잭션 커밋 시 락 해제.
     */
    private Payment prepareConfirmation(ConfirmPaymentCommand command) {
        Payment payment = paymentRepository.findByIdForUpdate(command.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "결제를 찾을 수 없습니다. paymentId=" + command.getPaymentId()
                ));

        if (payment.getUser() == null || !Objects.equals(payment.getUser().getId(), command.getUserId())) {
            throw new PaymentValidationException(
                    "결제를 확인할 권한이 없습니다. paymentId=" + command.getPaymentId()
            );
        }

        PaymentStatusTransitionPolicy.validateCanApprove(payment);
        return payment;
    }

    /**
     * 3단계: PG 결과에 따라 상태 반영 및 이벤트 발행(어댑터 afterCommit에서 발행).
     * 1단계→3단계 사이 다른 트랜잭션이 결제를 변경했을 수 있으므로, 반영 전 상태를 재검증한다.
     */
    private PaymentConfirmationResult applyConfirmationResult(
            ConfirmPaymentCommand command,
            Long paymentId,
            PaymentGatewayPort.PaymentGatewayResult confirmResult) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "결제를 찾을 수 없습니다. paymentId=" + paymentId
                ));

        // 1단계 커밋 후 다른 프로세스가 취소/환불 등으로 상태를 바꿨을 수 있음 → 재검증
        PaymentStatusTransitionPolicy.validateCanApprove(payment);

        if (!confirmResult.isSuccess()) {
            payment.markFailed("결제 제공자 승인 실패: " + confirmResult.getMessage());
            paymentRepository.save(payment);

            PaymentFailedEvent failedEvent = PaymentFailedEvent.of(
                    payment.getId(),
                    command.getUserId(),
                    PaymentErrorCode.GATEWAY_CONFIRM_FAILED.name(),
                    confirmResult.getErrorMessage()
            );
            eventPublisher.publishPaymentFailed(failedEvent);

            log.warn("결제 확인 실패: paymentId={}, message={}", payment.getId(), confirmResult.getMessage());
            return PaymentConfirmationResult.builder()
                    .paymentId(payment.getId())
                    .status(payment.getStatus())
                    .externalPaymentId(null)
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .build();
        }

        payment.approve(confirmResult.getExternalPaymentId());
        payment = paymentRepository.save(payment);

        PaymentConfirmedEvent event = PaymentConfirmedEvent.of(
                payment.getId(),
                command.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                confirmResult.getExternalPaymentId()
        );
        eventPublisher.publishPaymentConfirmed(event);

        log.info("결제 확인 완료: paymentId={}, status=SUCCESS", payment.getId());

        return PaymentConfirmationResult.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .externalPaymentId(confirmResult.getExternalPaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .build();
    }
}
