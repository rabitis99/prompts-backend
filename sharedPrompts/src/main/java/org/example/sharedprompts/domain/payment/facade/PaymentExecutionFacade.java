package org.example.sharedprompts.domain.payment.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.validator.PaymentValidator;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Payment Execution Facade
 * 
 * <p>단일 책임: 결제 실행만 담당
 * - Provider 호출 및 PaymentResult 변환
 * - PaymentValidator를 통한 검증
 * - Payment 도메인 메서드를 통한 상태 변경
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExecutionFacade {
    
    private final PaymentProviderFactory providerFactory;
    private final PaymentValidator paymentValidator;
    private final PaymentRepository paymentRepository;
    
    /**
     * 결제 실행
     * 
     * @param payment Payment 엔티티
     * @param actualAmount 실제 결제 금액 (포인트 사용 후)
     * @return PaymentResult
     */
    @Transactional
    public PaymentResult executePayment(Payment payment, BigDecimal actualAmount) {
        // 이미 SUCCESS 상태면 외부 API 재호출 금지 (멱등성)
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment가 이미 완료 상태: paymentId={}, externalPaymentId={}", 
                    payment.getId(), payment.getExternalPaymentId());
            return PaymentResult.builder()
                    .externalPaymentId(payment.getExternalPaymentId())
                    .status(PaymentStatus.SUCCESS)
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .orderId(String.valueOf(payment.getId()))
                    .approvedAt(payment.getApprovedAt())
                    .build();
        }
        
        // 멱등성 키 생성
        String idempotencyKey = generateIdempotencyKey(payment);
        payment.updateIdempotencyKey(idempotencyKey);
        
        // Provider 선택 및 결제 승인 호출
        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());
        
        PaymentResult result = provider.confirmPayment(
                payment.getExternalPaymentId() != null ? payment.getExternalPaymentId() : String.valueOf(payment.getId()),
                String.valueOf(payment.getId()),
                actualAmount,
                payment.getCurrency(),
                idempotencyKey
        );
        
        // PaymentResult 검증
        paymentValidator.validatePaymentResult(payment, result);
        
        // 도메인 메서드를 통한 상태 변경
        if (result.isSuccess()) {
            payment.markSuccess(result.getExternalPaymentId());
        } else {
            payment.markFailed(result.getFailureReason() != null ? result.getFailureReason() : "결제 승인 실패");
        }
        
        paymentRepository.save(payment);

        return result;
    }
    
    /**
     * 결제 취소 실행
     */
    @Transactional
    public void executeCancel(Payment payment, String reason) {
        if (payment.getExternalPaymentId() == null) {
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "외부 결제 ID가 없습니다.");
        }
        
        String idempotencyKey = generateIdempotencyKey(payment, "cancel");
        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());
        
        provider.cancelPayment(payment.getExternalPaymentId(), reason, idempotencyKey);
        
        payment.markCanceled();
        paymentRepository.save(payment);
    }
    
    /**
     * 결제 환불 실행
     */
    @Transactional
    public void executeRefund(Payment payment, BigDecimal refundAmount, String reason) {
        if (payment.getExternalPaymentId() == null) {
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "외부 결제 ID가 없습니다.");
        }
        
        String idempotencyKey = generateIdempotencyKey(payment, "refund");
        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());
        
        provider.refundPayment(payment.getExternalPaymentId(), refundAmount, reason, idempotencyKey);
        
        payment.refund(refundAmount);
        paymentRepository.save(payment);
    }
    
    /**
     * 멱등성 키 생성
     *
     * <p>결정론적 키 생성: 동일한 Payment에 대해 항상 같은 키 반환
     * - 기존에 저장된 idempotencyKey가 있으면 재사용
     * - 없으면 paymentMethod:paymentId 형식으로 생성
     */
    private String generateIdempotencyKey(Payment payment) {
        if (payment.getIdempotencyKey() != null) {
            return payment.getIdempotencyKey();
        }
        return String.format("%s:%s",
                payment.getPaymentMethod().name(),
                payment.getId());
    }

    /**
     * 멱등성 키 생성 (액션 포함)
     *
     * <p>취소/환불 등 특정 액션에 대한 결정론적 키 생성
     */
    private String generateIdempotencyKey(Payment payment, String action) {
        return String.format("%s:%s:%s",
                payment.getPaymentMethod().name(),
                payment.getId(),
                action);
    }
}

