package org.example.sharedprompts.domain.payment.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.port.in.command.ConfirmPaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentConfirmationResult;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.PaymentConfirmationUseCase;
import org.example.sharedprompts.domain.payment.application.port.out.event.PaymentEventPublisherPort;
import org.example.sharedprompts.domain.payment.application.port.out.paymentgateway.PaymentGatewayPort;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentCommandRepositoryPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.event.PaymentConfirmedEvent;
import org.example.sharedprompts.domain.payment.domain.event.PaymentFailedEvent;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentNotFoundException;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentValidationException;
import org.example.sharedprompts.domain.payment.domain.policy.PaymentStatusTransitionPolicy;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 확인 유스케이스 구현
 * 외부 결제 제공자로부터 토큰을 받아 최종 승인을 처리합니다
 */
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DefaultPaymentConfirmationService implements PaymentConfirmationUseCase {

    private final PaymentCommandRepositoryPort paymentRepository;
    private final PaymentGatewayPort paymentGateway;
    private final PaymentEventPublisherPort eventPublisher;

    @Override
    public PaymentConfirmationResult confirm(ConfirmPaymentCommand command) {
        log.info("결제 확인 시작: paymentId={}, userId={}", command.getPaymentId(), command.getUserId());

        // 1. 결제 조회 (락)
        Payment payment = paymentRepository.findByIdForUpdate(command.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "결제를 찾을 수 없습니다. paymentId=" + command.getPaymentId()
                ));

        // 2. 사용자 검증
        if (!payment.getUser().getId().equals(command.getUserId())) {
            throw new PaymentValidationException(
                    "결제를 확인할 권한이 없습니다. paymentId=" + command.getPaymentId()
            );
        }

        // 3. 상태 검증 (PENDING 상태여야 함)
        PaymentStatusTransitionPolicy.validateCanApprove(payment);

        try {
            // 4. 게이트웨이로 최종 승인 요청
            PaymentGatewayPort.PaymentGatewayResult confirmResult = paymentGateway.confirmPayment(
                    payment,
                    command.getProviderToken()
            );

            if (!confirmResult.success) {
                // 실패 처리
                payment.markFailed("결제 제공자 승인 실패: " + confirmResult.message);
                paymentRepository.save(payment);

                PaymentFailedEvent failedEvent = PaymentFailedEvent.of(
                        payment.getId(),
                        command.getUserId(),
                        "GATEWAY_CONFIRM_FAILED",
                        confirmResult.message
                );
                eventPublisher.publishPaymentFailed(failedEvent);

                throw new PaymentValidationException(
                        "결제 확인 중 오류가 발생했습니다: " + confirmResult.message,
                        confirmResult.exception
                );
            }

            // 5. 결제 상태를 SUCCESS로 업데이트
            payment.approve(confirmResult.externalPaymentId);
            payment = paymentRepository.save(payment);

            // 6. 확인 이벤트 발행 (트랜잭션 후)
            PaymentConfirmedEvent event = PaymentConfirmedEvent.of(
                    payment.getId(),
                    command.getUserId(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    confirmResult.externalPaymentId
            );
            eventPublisher.publishPaymentConfirmed(event);

            log.info("결제 확인 완료: paymentId={}, status=SUCCESS", payment.getId());

            return PaymentConfirmationResult.builder()
                    .paymentId(payment.getId())
                    .status(payment.getStatus())
                    .externalPaymentId(confirmResult.externalPaymentId)
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .build();

        } catch (Exception e) {
            log.error("결제 확인 중 오류 발생: paymentId={}, 오류: {}", command.getPaymentId(), e.getMessage(), e);
            throw e;
        }
    }
}
