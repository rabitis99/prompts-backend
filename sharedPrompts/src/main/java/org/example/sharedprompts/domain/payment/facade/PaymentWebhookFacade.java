package org.example.sharedprompts.domain.payment.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.webhook.WebhookHandler;
import org.example.sharedprompts.domain.payment.webhook.WebhookIdempotencyService;
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
    private final PaymentRepository paymentRepository;
    private final WebhookIdempotencyService idempotencyService;
    
    /**
     * Webhook 처리
     *
     * <p>트랜잭션 경계를 명확히 관리:
     * - WebhookHandler는 트랜잭션 없이 파싱/검증/락 획득만 수행
     * - Facade에서 트랜잭션 시작 후 상태 변경 및 저장
     * - DB 저장 성공 후 markAsProcessed() 호출 (Redis와 DB 일관성 보장)
     *
     * @param paymentMethod 결제 수단
     * @param payload Webhook 페이로드
     * @param signature 서명 (검증용)
     * @return 처리된 Payment (이미 처리된 경우 기존 Payment 반환)
     */
    @Transactional
    public Optional<Payment> handleWebhook(PaymentMethod paymentMethod, String payload, String signature) {
        // WebhookHandler를 통해 파싱, 검증, 멱등성 락 획득 (트랜잭션 없음)
        Optional<WebhookHandler.WebhookProcessingResult> resultOpt =
                webhookHandler.handleWebhook(paymentMethod, payload, signature);

        if (resultOpt.isPresent()) {
            WebhookHandler.WebhookProcessingResult result = resultOpt.get();
            Payment payment = result.payment();
            String webhookId = result.webhookId();

            try {
                // Webhook 이벤트에서 PaymentResult 추출 (중복 파싱 제거)
                if (result.webhookEvent().paymentResult() != null && payment.getStatus() == PaymentStatus.PENDING) {
                    // Payment 도메인 메서드를 통해 상태 변경
                    applyWebhookResult(payment, result.webhookEvent().paymentResult());
                }

                // 트랜잭션 내에서 저장
                payment = paymentRepository.save(payment);

                // DB 저장 성공 후 Redis에 처리 완료 마킹 (일관성 보장)
                idempotencyService.markAsProcessed(webhookId);

                return Optional.of(payment);
            } catch (Exception e) {
                // 트랜잭션 실패 시 락 해제 (재처리 가능하도록)
                idempotencyService.releaseProcessingLock(webhookId);
                log.error("Webhook 처리 중 트랜잭션 실패: webhookId={}, paymentId={}", webhookId, payment.getId(), e);
                throw e;
            }
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

