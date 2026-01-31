package org.example.sharedprompts.domain.payment.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.validator.PaymentValidator;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Webhook 처리 전용 컴포넌트
 * 
 * <p>단일 책임: Webhook/Callback 파싱, 검증, 멱등성 처리
 * - 실제 상태 변경은 PaymentService에 위임
 * - Provider 호출 전 검증 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookHandler {
    
    private final PaymentProviderFactory providerFactory;
    private final PaymentRepository paymentRepository;
    private final PaymentValidator paymentValidator;
    private final WebhookIdempotencyService idempotencyService;
    
    /**
     * Webhook 처리
     * 
     * @param paymentMethod 결제 수단
     * @param payload Webhook 페이로드
     * @param signature 서명 (검증용)
     * @return 처리된 Payment (이미 처리된 경우 기존 Payment 반환)
     */
    @Transactional
    public Optional<Payment> handleWebhook(PaymentMethod paymentMethod, String payload, String signature) {
        // 1. Provider 조회
        PaymentProvider provider = providerFactory.getProvider(paymentMethod);
        
        // 2. 서명 검증
        if (!provider.verifyWebhookSignature(payload, signature)) {
            log.error("Webhook 서명 검증 실패: paymentMethod={}", paymentMethod);
            throw new ApiException(ErrorCode.PAYMENT_WEBHOOK_SIGNATURE_INVALID);
        }
        
        // 3. Webhook 파싱
        PaymentProvider.WebhookEvent event = provider.parseWebhook(payload);
        
        // 4. 멱등성 확인 (Webhook ID 기반)
        String webhookId = generateWebhookId(paymentMethod, event);
        if (idempotencyService.isAlreadyProcessed(webhookId)) {
            log.info("Webhook 이미 처리됨: webhookId={}, externalPaymentId={}", 
                    webhookId, event.externalPaymentId());
            return paymentRepository.findByExternalPaymentId(event.externalPaymentId());
        }
        
        // 5. Payment 조회
        Optional<Payment> paymentOpt = paymentRepository.findByExternalPaymentId(event.externalPaymentId());
        if (paymentOpt.isEmpty()) {
            log.warn("Webhook 수신했으나 Payment를 찾을 수 없음: externalPaymentId={}", 
                    event.externalPaymentId());
            // Webhook ID는 저장하여 중복 처리 방지
            idempotencyService.markAsProcessed(webhookId);
            return Optional.empty();
        }
        
        Payment payment = paymentOpt.get();
        
        // 6. 이미 SUCCESS 상태면 재처리하지 않음 (멱등성)
        if (payment.getStatus().isCompleted()) {
            log.info("Payment가 이미 완료 상태: paymentId={}, status={}, externalPaymentId={}", 
                    payment.getId(), payment.getStatus(), event.externalPaymentId());
            idempotencyService.markAsProcessed(webhookId);
            return Optional.of(payment);
        }
        
        // 7. PaymentResult 검증
        if (event.paymentResult() != null) {
            paymentValidator.validatePaymentResult(payment, event.paymentResult());
        }
        
        // 8. Webhook ID 저장 (멱등성 보장)
        idempotencyService.markAsProcessed(webhookId);
        
        // 9. Payment 반환 (실제 상태 변경은 PaymentService에서 처리)
        return Optional.of(payment);
    }
    
    /**
     * Webhook ID 생성 (멱등성 키)
     */
    private String generateWebhookId(PaymentMethod paymentMethod, PaymentProvider.WebhookEvent event) {
        // 결제사별 고유 ID + 이벤트 타입으로 멱등성 키 생성
        return String.format("%s:%s:%s", 
                paymentMethod.name(), 
                event.externalPaymentId(), 
                event.eventType());
    }
}

