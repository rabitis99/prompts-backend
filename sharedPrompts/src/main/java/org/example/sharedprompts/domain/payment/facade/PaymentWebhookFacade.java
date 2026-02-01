package org.example.sharedprompts.domain.payment.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.webhook.WebhookHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Payment Webhook Facade
 * 
 * <p>단일 책임: Webhook 처리만 담당
 * - WebhookHandler를 통한 파싱, 검증, 멱등성 처리
 * - Payment 도메인 메서드를 통한 상태 변경
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentWebhookFacade {
    
    private final WebhookHandler webhookHandler;
    private final PaymentProviderFactory providerFactory;
    private final PaymentRepository paymentRepository;
    
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
        // WebhookHandler를 통해 파싱, 검증, 멱등성 처리
        Optional<Payment> paymentOpt = webhookHandler.handleWebhook(paymentMethod, payload, signature);
        
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            
            // Webhook 결과에 따라 상태 동기화
            // WebhookHandler에서 이미 검증 및 멱등성 처리가 완료되었으므로
            // Provider를 통해 Webhook 이벤트 파싱 후 상태 변경
            PaymentProvider provider = providerFactory.getProvider(paymentMethod);
            PaymentProvider.WebhookEvent event = provider.parseWebhook(payload);
            
            if (event.paymentResult() != null && payment.getStatus() == PaymentStatus.PENDING) {
                // Payment 도메인 메서드를 통해 상태 변경
                applyWebhookResult(payment, event.paymentResult());
            }
            
            payment = paymentRepository.save(payment);
            return Optional.of(payment);
        }
        
        return Optional.empty();
    }
    
    /**
     * Webhook 결과를 Payment에 적용 (내부 헬퍼)
     * 저장은 호출자가 담당
     */
    private void applyWebhookResult(Payment payment, PaymentResult paymentResult) {
        // 이미 SUCCESS 상태면 재처리하지 않음 (멱등성)
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment가 이미 완료 상태: paymentId={}, externalPaymentId={}",
                    payment.getId(), payment.getExternalPaymentId());
            return;
        }

        // Payment 도메인 메서드를 통해 상태 변경
        payment.applyWebhookResult(
                paymentResult.getExternalPaymentId(),
                paymentResult.getStatus(),
                paymentResult.getApprovedAt(),
                paymentResult.getFailureReason()
        );
    }
}

