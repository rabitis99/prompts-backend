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
        
        // 재시도 횟수 증가 및 다음 재시도 시간 예약
        payment.incrementRetryCount();
        payment.scheduleNextRetry(BASE_RETRY_DELAY_MS);
        payment.markInProgress(); // 재시도 시 PENDING 상태로 변경
        
        payment = paymentRepository.save(payment);
        
        // PaymentExecutionService를 통한 재시도 실행
        return executionService.executePayment(payment, payment.getAmount());
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
     * @return 처리된 결제 수
     */
    @Transactional
    public int processRetryablePayments() {
        List<Payment> retryablePayments = findRetryablePayments();
        int processedCount = 0;
        
        for (Payment payment : retryablePayments) {
            try {
                retryPayment(payment.getId());
                processedCount++;
                log.info("결제 재시도 성공: paymentId={}, retryCount={}", 
                        payment.getId(), payment.getRetryCount());
            } catch (Exception e) {
                log.error("결제 재시도 실패: paymentId={}, error={}", 
                        payment.getId(), e.getMessage(), e);
            }
        }
        
        return processedCount;
    }
}

