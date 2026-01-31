package org.example.sharedprompts.domain.payment.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.execution.PaymentExecutionService;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    
    private static final int MAX_RETRY_COUNT = 3;
    private static final long BASE_RETRY_DELAY_MS = 1000L; // 기본 1초
    
    private final PaymentRepository paymentRepository;
    private final PaymentExecutionService executionService;
    
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
        if (!payment.isRetryable(MAX_RETRY_COUNT)) {
            throw new ApiException(ErrorCode.PAYMENT_RETRY_EXCEEDED);
        }
        
        // 재시도 상태 확정: 별도 트랜잭션에서 먼저 커밋
        // executePayment() 실패 여부와 관계없이 재시도 상태는 반드시 DB에 반영됨
        Payment updatedPayment = commitRetryState(payment);
        
        // PaymentExecutionService를 통한 재시도 실행
        // 실패 시 ApiException이 상위로 전파되지만, 재시도 상태는 이미 커밋됨
        Payment executedPayment = executionService.executePayment(updatedPayment, updatedPayment.getAmount());

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
    
    /**
     * 재시도 상태 확정
     * 
     * <p>재시도 횟수 증가, 다음 재시도 시간 예약, 상태 변경을 별도 트랜잭션에서 커밋합니다.
     * REQUIRES_NEW 전파 속성을 사용하여 메인 트랜잭션과 독립적으로 실행되며,
     * executePayment() 실패 여부와 관계없이 재시도 상태는 반드시 DB에 반영됩니다.
     * 
     * @param payment Payment 엔티티
     * @return 저장된 Payment 엔티티
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment commitRetryState(Payment payment) {
        // 재시도 횟수 증가 및 다음 재시도 시간 예약
        payment.incrementRetryCount();
        payment.scheduleNextRetry(BASE_RETRY_DELAY_MS);
        payment.markInProgress(); // 재시도 시 PENDING 상태로 변경
        
        return paymentRepository.save(payment);
    }
    
    /**
     * 재시도 가능한 결제 목록 조회
     * 
     * @return 재시도 가능한 Payment 목록
     */
    @Transactional(readOnly = true)
    public List<Payment> findRetryablePayments() {
        LocalDateTime now = LocalDateTime.now();
        return paymentRepository.findRetryablePayments(MAX_RETRY_COUNT, now);
    }
    
    /**
     * 재시도 가능한 결제 일괄 처리
     *
     * <p>각 재시도는 독립적인 트랜잭션에서 실행됨
     * - 개별 재시도 실패가 다른 재시도에 영향을 주지 않음
     * - retryPayment()의 @Transactional이 각각 적용됨
     *
     * @return 처리된 결제 수
     */
    public int processRetryablePayments() {
        List<Payment> retryablePayments = findRetryablePayments();
        int processedCount = 0;

        for (Payment payment : retryablePayments) {
            Long paymentId = payment.getId();
            int previousRetryCount = payment.getRetryCount();
            try {
                retryPayment(paymentId);
                processedCount++;
                log.info("결제 재시도 성공: paymentId={}, retryCount={}",
                        paymentId, previousRetryCount + 1);
            } catch (Exception e) {
                log.error("결제 재시도 실패: paymentId={}, error={}",
                        paymentId, e.getMessage(), e);
            }
        }

        return processedCount;
    }
}

