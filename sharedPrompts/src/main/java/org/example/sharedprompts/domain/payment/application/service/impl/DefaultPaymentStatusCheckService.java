package org.example.sharedprompts.domain.payment.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.port.in.command.PaymentStatusCheckQuery;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentStatusResult;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.PaymentStatusCheckUseCase;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentQueryRepositoryPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 상태 조회 유스케이스 구현
 */
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultPaymentStatusCheckService implements PaymentStatusCheckUseCase {

    private final PaymentQueryRepositoryPort paymentRepository;

    @Override
    public PaymentStatusResult checkStatus(PaymentStatusCheckQuery query) {
        log.info("결제 상태 조회 시작: paymentId={}, userId={}", query.getPaymentId(), query.getUserId());

        Payment payment = paymentRepository.findById(query.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "결제를 찾을 수 없습니다. paymentId=" + query.getPaymentId()
                ));

        // 사용자 검증 (자신의 결제만 조회 가능)
        if (!payment.getUser().getId().equals(query.getUserId())) {
            throw new PaymentNotFoundException(
                    "결제를 찾을 수 없습니다 (권한 없음). paymentId=" + query.getPaymentId()
            );
        }

        log.info("결제 상태 조회 완료: paymentId={}, status={}", query.getPaymentId(), payment.getStatus());

        return PaymentStatusResult.builder()
                .id(payment.getId())
                .userId(payment.getUser().getId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .createdAt(payment.getCreatedAt())
                .approvedAt(payment.getApprovedAt())
                .canceledAt(payment.getCanceledAt())
                .build();
    }
}
