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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 결제 취소 유스케이스 구현
 */
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DefaultPaymentCancellationService implements PaymentCancellationUseCase {

    private final PaymentCommandRepositoryPort paymentRepository;
    private final PaymentGatewayPort paymentGateway;
    private final PaymentEventPublisherPort eventPublisher;

    @Override
    public PaymentCancellationResult cancel(CancelPaymentCommand command) {
        log.info("결제 취소 시작: paymentId={}, userId={}, reason={}",
                command.getPaymentId(), command.getUserId(), command.getReason());

        // 1. 결제 조회 (락)
        Payment payment = paymentRepository.findByIdForUpdate(command.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "결제를 찾을 수 없습니다. paymentId=" + command.getPaymentId()
                ));

        // 2. 사용자 검증
        if (payment.getUser() == null || !payment.getUser().getId().equals(command.getUserId())) {
            throw new PaymentValidationException(
                    "결제를 취소할 권한이 없습니다. paymentId=" + command.getPaymentId()
            );
        }

        // 3. 상태 검증
        PaymentStatusTransitionPolicy.validateCanCancel(payment);

        // 4. 취소 금액 계산
        BigDecimal cancelAmount = PaymentStatusTransitionPolicy.calculateRefundableAmount(payment);

        // 5. 외부 PG에 취소 요청 (SUCCESS 상태인 경우만)
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            PaymentGatewayPort.PaymentGatewayResult gatewayResult = paymentGateway.cancelPayment(payment);
            if (!gatewayResult.isSuccess()) {
                throw new PaymentValidationException(
                        "결제 취소 중 오류가 발생했습니다: " + gatewayResult.getMessage(),
                        gatewayResult.getException()
                );
            }
        }

        // 6. 결제 상태 업데이트
        payment.markCanceled();
        payment = paymentRepository.save(payment);

        // 7. 이벤트 발행 (트랜잭션 후)
        PaymentCanceledEvent event = PaymentCanceledEvent.of(
                payment.getId(),
                payment.getUser().getId(),
                cancelAmount,
                command.getReason()
        );

        log.info("결제 취소 완료: paymentId={}, cancelAmount={}", payment.getId(), cancelAmount);

        eventPublisher.publishPaymentCanceled(event);

        return PaymentCancellationResult.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .refundedAmount(cancelAmount)
                .reason(command.getReason())
                .build();
    }
}
