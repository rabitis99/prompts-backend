package org.example.sharedprompts.domain.payment.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.properties.RetryProperties;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.execution.PaymentExecutionService;
import org.example.sharedprompts.domain.payment.service.payment.PaymentRetryService;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
     * @param payment Payment 엔티티
     * @return 저장된 Payment 엔티티
     */
    public Payment commitRetryState(Payment payment) {
        return paymentRetryService.commitRetryState(payment.getId());
    }

    /**
     * 결제 재시도
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

        Payment updatedPayment = commitRetryState(payment);
        
        // 실제 결제 금액 계산 (포인트 사용 후 금액)
        BigDecimal actualAmount = updatedPayment.getAmount().subtract(
                updatedPayment.getUsedPointAmount() != null ? updatedPayment.getUsedPointAmount() : java.math.BigDecimal.ZERO
        );

        Payment executedPayment = executionService.executePayment(updatedPayment, actualAmount);

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

    /**
     * 결제 재시도 (PaymentResponseDto 반환)
     * 
     * <p>PaymentFacade에서 사용하기 위한 오버로드 메서드
     * 
     * @param paymentId 결제 ID
     * @return PaymentResponseDto
     */
    @Transactional
    public PaymentResponseDto retryPaymentAsResponse(Long paymentId) {
        // 재시도 실행
        retryPayment(paymentId);
        
        // 재시도 후 최신 Payment 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        return PaymentResponseDto.from(payment);
    }
    
}

