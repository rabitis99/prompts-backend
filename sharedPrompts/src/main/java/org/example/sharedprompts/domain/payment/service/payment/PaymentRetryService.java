package org.example.sharedprompts.domain.payment.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 상태 업데이트 전용 서비스
 * - REQUIRES_NEW 트랜잭션으로 개별 결제 상태 업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRetryService {

    private final PaymentRepository paymentRepository;
    private final PaymentProperties paymentProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePaymentStatusSuccess(Long paymentId, String externalPaymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        log.info("결제 재시도 성공: paymentId={}, externalPaymentId={}, retryCount={}", 
                paymentId, externalPaymentId, payment.getRetryCount());
        
        payment.approve(externalPaymentId);
        paymentRepository.save(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePaymentStatusFailure(Long paymentId, String failureReason, long retryDelayMs) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        payment.incrementRetryCount();
        int currentRetryCount = payment.getRetryCount();
        int maxRetryAttempts = paymentProperties.getMaxRetryAttempts();
        
        // 재시도 가능 여부 확인
        if (payment.isRetryable(maxRetryAttempts)) {
            // 지수 백오프 적용하여 다음 재시도 시간 예약
            payment.scheduleNextRetry(retryDelayMs);
            log.warn("결제 재시도 예약: paymentId={}, retryCount={}/{}, failureReason={}, nextRetryAt={}, retryDelayMs={}", 
                    paymentId, currentRetryCount, maxRetryAttempts, failureReason, 
                    payment.getNextRetryAt(), retryDelayMs);
        } else {
            // 최대 재시도 횟수 초과 시 실패 처리
            String finalFailureReason = failureReason != null ? failureReason : "재시도 실패";
            payment.fail(finalFailureReason);
            log.error("결제 재시도 최종 실패: paymentId={}, retryCount={}/{}, failureReason={}, maxRetryAttempts={}", 
                    paymentId, currentRetryCount, maxRetryAttempts, finalFailureReason, maxRetryAttempts);
        }
        paymentRepository.save(payment);
    }
}
