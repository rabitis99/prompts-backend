package org.example.sharedprompts.domain.payment.application.command.service.confirm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.example.sharedprompts.dto.payment.response.PaymentConfirmResponse;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConfirmOptimisticLockHandler {

    private final PaymentJpaAdapter paymentJpaAdapter;

    public PaymentExecutionResult handleOptimisticLockInTransaction(Long paymentId,
                                                                    PaymentConfirmRequest request,
                                                                    ObjectOptimisticLockingFailureException e) {
        log.info("결제 승인 낙관적 락 충돌(트랜잭션 본문), 최신 Payment 재조회: paymentId={}", paymentId);
        Payment fresh = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        if (fresh.getStatus() == PaymentStatus.SUCCESS) {
            return new PaymentExecutionResult(fresh, 0, true, null, null);
        }
        throw e;
    }

    public PaymentConfirmResponse handleOptimisticLockException(Long paymentId,
                                                               PaymentConfirmRequest request,
                                                               ObjectOptimisticLockingFailureException e,
                                                               PaymentConfirmResponseMapper responseMapper) {
        log.info("결제 승인 낙관적 락 충돌(커밋 시점), 최신 Payment 재조회: paymentId={}", paymentId);
        Payment fresh = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        if (fresh.getStatus() == PaymentStatus.SUCCESS) {
            return responseMapper.toConfirmResponse(fresh, request);
        }
        throw e;
    }
}

