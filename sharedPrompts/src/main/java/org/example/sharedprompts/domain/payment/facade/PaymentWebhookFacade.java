package org.example.sharedprompts.domain.payment.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.provider.webhook.PaymentWebhookHandler;
import org.example.sharedprompts.domain.payment.provider.webhook.PaymentWebhookHandlerFactory;
import org.example.sharedprompts.domain.payment.provider.webhook.WebhookEvent;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.lock.DistributedLockService;
import org.example.sharedprompts.domain.payment.service.webhook.PaymentWebhookTransactionService;
import org.example.sharedprompts.domain.payment.webhook.WebhookIdempotencyService;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Payment Webhook Facade
 *
 * <p>단일 책임: Webhook 처리만 담당
 * <ul>
 *   <li>DB 기반 멱등성 검사 (Payment 상태 확인)</li>
 *   <li>Redis 락은 동시 처리 방지용으로만 사용</li>
 *   <li>트랜잭션 경계 명확히 분리</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentWebhookFacade {

    private final PaymentWebhookHandlerFactory webhookHandlerFactory;
    private final PaymentRepository paymentRepository;
    private final PaymentWebhookTransactionService transactionService;
    private final WebhookIdempotencyService idempotencyService;
    private final DistributedLockService distributedLockService;

    /**
     * Webhook 처리 (메인 진입점)
     */
    public Optional<Payment> handleWebhook(PaymentMethod paymentMethod, String payload, String signature) {
        return handleWebhook(paymentMethod, payload, signature, java.util.Collections.emptyMap());
    }

    /**
     * Webhook 처리 (메인 진입점) - headers 포함 버전
     */
    public Optional<Payment> handleWebhook(
            PaymentMethod paymentMethod,
            String payload,
            String signature,
            java.util.Map<String, String> headers
    ) {
        PaymentWebhookHandler handler = webhookHandlerFactory.getHandler(paymentMethod);
        WebhookEvent event = parseAndVerifyWebhook(handler, payload, signature, headers, paymentMethod);

        String webhookId = generateWebhookId(paymentMethod, event);
        String externalPaymentId = event.externalPaymentId();

        RedisLockContext lockContext = acquireRedisLock(webhookId);

        try {
            Optional<Payment> paymentOpt = paymentRepository.findByExternalPaymentId(externalPaymentId);
            if (paymentOpt.isEmpty()) {
                log.warn("Webhook 수신했으나 Payment를 찾을 수 없음: externalPaymentId={}", externalPaymentId);
                return Optional.empty();
            }

            Payment payment = paymentOpt.get();

            if (isAlreadyProcessed(payment)) {
                log.info("Payment 이미 처리 완료: paymentId={}, status={}", payment.getId(), payment.getStatus());
                return Optional.of(payment);
            }

            String lockKey = distributedLockService.createLockKey("payment", payment.getId()) + ":state";
            Payment processedPayment = distributedLockService.executeWithLock(lockKey,
                    () -> transactionService.processPaymentInTransaction(payment, event));

            markRedisAsProcessed(lockContext);
            return Optional.of(processedPayment);

        } catch (Exception e) {
            log.error("Webhook 처리 실패: webhookId={}, externalPaymentId={}", webhookId, externalPaymentId, e);
            throw e;
        } finally {
            releaseRedisLock(lockContext);
        }
    }

    /**
     * Webhook 파싱 및 서명 검증
     */
    private WebhookEvent parseAndVerifyWebhook(
            PaymentWebhookHandler handler,
            String payload,
            String signature,
            java.util.Map<String, String> headers,
            PaymentMethod paymentMethod
    ) {
        boolean verified = (headers != null && !headers.isEmpty())
                ? handler.verifyWebhookSignature(payload, headers)
                : handler.verifyWebhookSignature(payload, signature);

        if (!verified) {
            log.error("Webhook 서명 검증 실패: paymentMethod={}", paymentMethod);
            throw new ApiException(ErrorCode.PAYMENT_WEBHOOK_SIGNATURE_INVALID);
        }

        try {
            return handler.parseWebhook(payload);
        } catch (RuntimeException e) {
            log.error("Webhook 파싱 실패: paymentMethod={}, error={}", paymentMethod, e.getMessage());
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_RESPONSE_INVALID,
                    "Webhook payload 파싱 실패: " + e.getMessage());
        }
    }

    /**
     * Redis 락 획득
     */
    private RedisLockContext acquireRedisLock(String webhookId) {
        try {
            boolean acquired = idempotencyService.tryAcquireLock(webhookId);
            return new RedisLockContext(webhookId, acquired);
        } catch (Exception e) {
            log.warn("Redis 락 획득 실패, DB 기반 멱등성으로 진행: webhookId={}", webhookId);
            return new RedisLockContext(webhookId, false);
        }
    }

    /**
     * Redis에 처리 완료 마킹
     */
    private void markRedisAsProcessed(RedisLockContext context) {
        if (!context.isAcquired()) {
            return;
        }
        try {
            idempotencyService.markAsProcessed(context.getWebhookId());
        } catch (Exception e) {
            log.warn("Redis 처리 완료 마킹 실패 (TTL로 자동 만료): webhookId={}", context.getWebhookId());
        }
    }

    /**
     * Redis 락 해제
     */
    private void releaseRedisLock(RedisLockContext context) {
        if (!context.isAcquired()) {
            return;
        }
        try {
            idempotencyService.releaseLock(context.getWebhookId());
        } catch (Exception e) {
            log.warn("Redis 락 해제 실패 (TTL로 자동 만료): webhookId={}", context.getWebhookId());
        }
    }

    /**
     * DB 기반 멱등성 검사: Payment 상태로 이미 처리되었는지 확인
     */
    private boolean isAlreadyProcessed(Payment payment) {
        return payment.getStatus() != PaymentStatus.PENDING;
    }

    /**
     * Redis 락 컨텍스트
     */
    private static class RedisLockContext {
        private final String webhookId;
        private final boolean acquired;

        RedisLockContext(String webhookId, boolean acquired) {
            this.webhookId = webhookId;
            this.acquired = acquired;
        }

        String getWebhookId() {
            return webhookId;
        }

        boolean isAcquired() {
            return acquired;
        }
    }

    /**
     * Webhook ID 생성 (멱등성 키)
     */
    private String generateWebhookId(PaymentMethod paymentMethod, WebhookEvent event) {
        return String.format("%s:%s:%s",
                paymentMethod.name(),
                event.externalPaymentId(),
                event.eventType());
    }
}
