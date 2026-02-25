package org.example.sharedprompts.domain.payment.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.port.in.command.PaymentHistoryQuery;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentHistoryResult;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.PaymentHistoryQueryUseCase;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentQueryRepositoryPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 결제 내역 조회 유스케이스 구현
 */
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultPaymentHistoryQueryService implements PaymentHistoryQueryUseCase {

    private final PaymentQueryRepositoryPort paymentRepository;

    @Override
    public Page<PaymentHistoryResult> getHistory(PaymentHistoryQuery query) {
        if (query == null || query.getUserId() == null || query.getPageable() == null) {
            throw new IllegalArgumentException("query.userId, query.pageable 는 필수입니다.");
        }
        log.info("결제 내역 조회 시작: userId={}, page={}, size={}",
                query.getUserId(), query.getPageable().getPageNumber(), query.getPageable().getPageSize());

        Page<Payment> paymentPage = paymentRepository.findByUserId(query.getUserId(), query.getPageable());

        List<PaymentHistoryResult> results = paymentPage.getContent().stream()
                .map(payment -> PaymentHistoryResult.builder()
                        .id(payment.getId())
                        .amount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .status(payment.getStatus())
                        .createdAt(payment.getCreatedAt())
                        .approvedAt(payment.getApprovedAt())
                        .build())
                .collect(Collectors.toList());

        log.info("결제 내역 조회 완료: userId={}, totalElements={}, totalPages={}",
                query.getUserId(), paymentPage.getTotalElements(), paymentPage.getTotalPages());

        return new PageImpl<>(results, query.getPageable(), paymentPage.getTotalElements());
    }
}
