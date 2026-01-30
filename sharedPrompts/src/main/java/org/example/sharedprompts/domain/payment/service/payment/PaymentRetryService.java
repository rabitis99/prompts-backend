package org.example.sharedprompts.domain.payment.service.payment;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.example.sharedprompts.domain.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 상태 업데이트 전용 서비스
 * - REQUIRES_NEW 트랜잭션으로 개별 결제 상태 업데이트
 */
@Service
@RequiredArgsConstructor
public class PaymentRetryService {

    private final PaymentRepository paymentRepository;
    private final PaymentProperties paymentProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePaymentStatusSuccess(Long paymentId, String externalPaymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("결제를 찾을 수 없음: " + paymentId));
        payment.approve(externalPaymentId);
        paymentRepository.save(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePaymentStatusFailure(Long paymentId, String failureReason, long retryDelayMs) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("결제를 찾을 수 없음: " + paymentId));
        payment.incrementRetryCount();
        
        // 재시도 가능 여부 확인
        if (payment.isRetryable(paymentProperties.getMaxRetryAttempts())) {
            // 지수 백오프 적용하여 다음 재시도 시간 예약
            payment.scheduleNextRetry(retryDelayMs);
        } else {
            // 최대 재시도 횟수 초과 시 실패 처리
            payment.fail(failureReason != null ? failureReason : "재시도 실패");
        }
        paymentRepository.save(payment);
    }
}
