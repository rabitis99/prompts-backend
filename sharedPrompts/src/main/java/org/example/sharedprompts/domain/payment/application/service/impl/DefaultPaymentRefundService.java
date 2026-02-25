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
import org.example.sharedprompts.domain.payment.domain.event.PaymentRefundedEvent;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentNotFoundException;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentValidationException;
import org.example.sharedprompts.domain.payment.domain.policy.PaymentStatusTransitionPolicy;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 결제 환불 유스케이스 구현
 */
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DefaultPaymentRefundService implements PaymentRefundUseCase {

    private final PaymentCommandRepositoryPort paymentRepository;
    private final PaymentGatewayPort paymentGateway;
    private final PaymentEventPublisherPort eventPublisher;

    @Override
    public PaymentRefundResult refund(RefundPaymentCommand command) {
        log.info("결제 환불 시작: paymentId={}, userId={}, refundAmount={}",
                command.getPaymentId(), command.getUserId(), command.getRefundAmount());

        // 1. 결제 조회 (락)
        Payment payment = paymentRepository.findByIdForUpdate(command.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "결제를 찾을 수 없습니다. paymentId=" + command.getPaymentId()
                ));

        // 2. 사용자 검증
        if (!payment.getUser().getId().equals(command.getUserId())) {
            throw new PaymentValidationException(
                    "결제를 환불할 권한이 없습니다. paymentId=" + command.getPaymentId()
            );
        }

        // 3. 상태 검증
        PaymentStatusTransitionPolicy.validateCanRefund(payment);

        // 4. 환불 금액 계산
        BigDecimal refundAmount = command.getRefundAmount();
        BigDecimal refundableAmount = PaymentStatusTransitionPolicy.calculateRefundableAmount(payment);

        if (refundAmount == null) {
            // 전체 환불
            refundAmount = refundableAmount;
        } else {
            // 부분 환불의 경우 금액 검증
            if (refundAmount.compareTo(BigDecimal.ZERO) <= 0 ||
                    refundAmount.compareTo(refundableAmount) > 0) {
                throw new PaymentValidationException(
                        "유효하지 않은 환불 금액입니다. 환불 가능: " + refundableAmount + ", 요청: " + refundAmount
                );
            }
        }

        // 5. 외부 PG에 환불 요청
        PaymentGatewayPort.PaymentGatewayResult gatewayResult = paymentGateway.refundPayment(payment, refundAmount);
        if (!gatewayResult.success) {
            throw new PaymentValidationException(
                    "결제 환불 중 오류가 발생했습니다: " + gatewayResult.message,
                    gatewayResult.exception
            );
        }

        // 6. 결제 상태 및 환불 금액 업데이트
        payment.refund(refundAmount);
        payment = paymentRepository.save(payment);

        // 7. 이벤트 발행 (트랜잭션 후)
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
}
