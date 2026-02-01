package org.example.sharedprompts.domain.payment.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.config.RetryProperties;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.execution.PaymentExecutionService;
import org.example.sharedprompts.domain.payment.service.payment.PaymentRetryService;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment Retry Facade
 * 
 * <p>단일 책임: 실패 결제 재시도만 담당
 * - 재시도 가능한 Payment 조회
 * - 재시도 횟수 및 백오프 관리
 * - PaymentExecutionService를 통한 재시도 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRetryFacade {

    private final PaymentRepository paymentRepository;
    private final PaymentExecutionService executionService;
    private final PaymentLoggingService loggingService;
    private final RetryProperties retryProperties;
    private final PaymentRetryService paymentRetryService;

    /**
     * 재시도 상태 확정
     * 
     * <p>재시도 횟수 증가, 다음 재시도 시간 예약, 상태 변경을 별도 트랜잭션(REQUIRES_NEW)에서 먼저 커밋합니다.
     * executePayment() 실패 여부와 관계없이 재시도 상태는 반드시 DB에 반영됩니다.
     * 이 시점에서 retryCount가 증가하므로, executePayment() 내부의 retryCount > 0 체크가 올바르게 작동합니다.
     * 
     * <p>PaymentRetryScheduler와 PaymentRetryFacade.retryPayment()에서 공통으로 사용하여
     * 두 재시도 경로의 트랜잭션 전략을 일관성 있게 유지합니다.
     * 
     * @param payment Payment 엔티티
     * @return 저장된 Payment 엔티티
     */
    public Payment commitRetryState(Payment payment) {
        return paymentRetryService.commitRetryState(payment);
    }

    /**
     * 결제 재시도
     * 
     * <p>트랜잭션 전략:
     * 1. 재시도 상태 확정(횟수 증가, 다음 재시도 시간, 상태 변경)은 별도 트랜잭션(REQUIRES_NEW)에서 먼저 커밋
     * 2. 결제 실행은 메인 트랜잭션에서 수행하며, 실패 시 예외 전파
     * 
     * <p>이를 통해 executePayment() 실패 여부와 관계없이
     * 재시도 관련 상태 변경(retryCount, nextRetryAt, status)은 반드시 DB에 반영됩니다.
     * 
     * @param paymentId 결제 ID
     * @return PaymentResult
     */
    @Transactional
    public PaymentResult retryPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        // 재시도 가능 여부 확인
        if (!payment.isRetryable(retryProperties.getMaxAttempts())) {
            throw new ApiException(ErrorCode.PAYMENT_RETRY_EXCEEDED);
        }
        
        // 재시도는 이전 시도에서 받은 paymentKey가 있어야 가능
        // paymentKey가 없으면 재시도 불가 (새로운 결제 요청 필요)
        if (payment.getExternalPaymentId() == null || payment.getExternalPaymentId().isEmpty()) {
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR,
                    "재시도할 수 없습니다. paymentKey가 없습니다. 새로운 결제를 요청해주세요.");
        }
        
        // 재시도 로깅
        loggingService.logRetryAttempt(payment, payment.getRetryCount() + 1);
        
        // 재시도 상태 확정: 별도 트랜잭션에서 먼저 커밋
        // executePayment() 실패 여부와 관계없이 재시도 상태는 반드시 DB에 반영됨
        // 이 시점에서 retryCount가 증가하므로, executePayment() 내부의 retryCount > 0 체크가 올바르게 작동
        Payment updatedPayment = commitRetryState(payment);
        
        // 실제 결제 금액 계산 (포인트 사용 후 금액)
        java.math.BigDecimal actualAmount = updatedPayment.getAmount().subtract(
                updatedPayment.getUsedPointAmount() != null ? updatedPayment.getUsedPointAmount() : java.math.BigDecimal.ZERO
        );
        
        // PaymentExecutionService를 통한 재시도 실행
        // 실패 시 ApiException이 상위로 전파되지만, 재시도 상태는 이미 커밋됨
        Payment executedPayment = executionService.executePayment(updatedPayment, actualAmount);

        // Payment에서 PaymentResult 생성
        return PaymentResult.builder()
                .externalPaymentId(executedPayment.getExternalPaymentId())
                .status(executedPayment.getStatus())
                .amount(executedPayment.getAmount())
                .currency(executedPayment.getCurrency())
                .orderId(String.valueOf(executedPayment.getId()))
                .approvedAt(executedPayment.getApprovedAt())
                .failureReason(executedPayment.getFailureReason())
                .build();
    }
    
}

