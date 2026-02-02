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

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Webhook 처리 전용 컴포넌트
 *
 * <p>단일 책임: Webhook/Callback 파싱, 검증, 멱등성 처리
 * - 실제 상태 변경은 PaymentService에 위임
 * - Provider 호출 전 검증 수행
 * - 트랜잭션 없이 순수하게 파싱/검증만 수행 (트랜잭션은 Facade 레벨에서 관리)
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
     * Webhook 처리 결과
     *
     * @param payment Payment 엔티티
     * @param webhookEvent Webhook 이벤트 (중복 파싱 방지)
     * @param webhookId Webhook ID (멱등성 키) - Facade에서 markAsProcessed 호출 시 사용
     */
    public record WebhookProcessingResult(Payment payment, PaymentProvider.WebhookEvent webhookEvent, String webhookId) {}

    /**
     * Webhook 처리
     *
     * <p>트랜잭션 없이 파싱, 검증, 멱등성 락 획득만 수행
     * 실제 상태 변경과 트랜잭션 관리는 Facade 레벨에서 담당
     *
     * <p><strong>중요:</strong> markAsProcessed()는 호출하지 않습니다.
     * Facade에서 DB 저장이 성공한 뒤에 호출해야 Redis와 DB 간 일관성이 보장됩니다.
     *
     * @param paymentMethod 결제 수단
     * @param payload Webhook 페이로드
     * @param signature 서명 (검증용)
     * @return 처리된 Payment, WebhookEvent, webhookId (이미 처리된 경우 기존 Payment 반환)
     */
    public Optional<WebhookProcessingResult> handleWebhook(PaymentMethod paymentMethod, String payload, String signature) {
        // 1. Provider 조회
        PaymentProvider provider = providerFactory.getProvider(paymentMethod);
        
        // 2. 서명 검증
        if (!provider.verifyWebhookSignature(payload, signature)) {
            log.error("Webhook 서명 검증 실패: paymentMethod={}", paymentMethod);
            throw new ApiException(ErrorCode.PAYMENT_WEBHOOK_SIGNATURE_INVALID);
        }
        
        // 3. Webhook 파싱
        PaymentProvider.WebhookEvent event = provider.parseWebhook(payload);
        
        // 4. 멱등성 확인 (Webhook ID 기반) - 원자적 락 획득
        String webhookId = generateWebhookId(paymentMethod, event);
        if (!idempotencyService.tryProcess(webhookId)) {
            log.info("Webhook 이미 처리됨: webhookId={}, externalPaymentId={}",
                    webhookId, event.externalPaymentId());
            return paymentRepository.findByExternalPaymentId(event.externalPaymentId())
                    .map(payment -> new WebhookProcessingResult(payment, event, webhookId));
        }
        
        try {
            // 5. Payment 조회
            Optional<Payment> paymentOpt = paymentRepository.findByExternalPaymentId(event.externalPaymentId());
            if (paymentOpt.isEmpty()) {
                log.warn("Webhook 수신했으나 Payment를 찾을 수 없음: externalPaymentId={}",
                        event.externalPaymentId());
                // 락 해제 (Facade에서 markAsProcessed 호출 없이 종료될 것이므로)
                idempotencyService.releaseProcessingLock(webhookId);
                return Optional.empty();
            }

            Payment payment = paymentOpt.get();

            // 6. 이미 SUCCESS 상태면 재처리하지 않음 (멱등성)
            if (payment.getStatus().isCompleted()) {
                log.info("Payment가 이미 완료 상태: paymentId={}, status={}, externalPaymentId={}",
                        payment.getId(), payment.getStatus(), event.externalPaymentId());
                // 이미 완료된 경우에도 webhookId 반환 (Facade에서 markAsProcessed 호출)
                return Optional.of(new WebhookProcessingResult(payment, event, webhookId));
            }

            // 7. PaymentResult 검증
            if (event.paymentResult() != null) {
                // 실제 결제 금액 계산 (포인트 사용 후 금액)
                BigDecimal actualAmount = payment.getAmount().subtract(
                        payment.getUsedPointAmount() != null ? payment.getUsedPointAmount() : java.math.BigDecimal.ZERO
                );
                paymentValidator.validatePaymentResult(payment, event.paymentResult(), actualAmount);
            }

            // 8. Payment, WebhookEvent, webhookId 반환 (중복 파싱 방지)
            // markAsProcessed()는 Facade에서 DB 저장 성공 후 호출 (일관성 보장)
            return Optional.of(new WebhookProcessingResult(payment, event, webhookId));
        } catch (Exception e) {
            // 처리 실패 시 락 해제
            idempotencyService.releaseProcessingLock(webhookId);
            log.error("Webhook 처리 중 오류 발생: webhookId={}, externalPaymentId={}", 
                    webhookId, event.externalPaymentId(), e);
            throw e;
        }
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

