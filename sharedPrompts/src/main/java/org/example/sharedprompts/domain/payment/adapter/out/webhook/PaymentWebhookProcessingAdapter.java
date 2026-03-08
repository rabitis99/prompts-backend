package org.example.sharedprompts.domain.payment.adapter.out.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.port.out.webhook.PaymentWebhookProcessingPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.webhook.WebhookEvent;
import org.example.sharedprompts.domain.payment.infrastructure.messaging.webhook.WebhookHandler;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.infrastructure.transaction.DistributedLockService;
import org.example.sharedprompts.domain.payment.infrastructure.messaging.webhook.PaymentWebhookTransactionService;
import org.example.sharedprompts.domain.payment.infrastructure.messaging.webhook.WebhookIdempotencyService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentWebhookProcessingAdapter implements PaymentWebhookProcessingPort {

    private final WebhookHandler webhookHandler;
    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentWebhookTransactionService transactionService;
    private final WebhookIdempotencyService idempotencyService;
    private final DistributedLockService distributedLockService;

    @Override
    public Optional<Payment> processWebhook(
            PaymentMethod paymentMethod,
            String payload,
            String signature,
            Map<String, String> headers
    ) {
        WebhookEvent event = webhookHandler.handle(paymentMethod, payload, signature, headers);

        String webhookId = generateWebhookId(paymentMethod, event);
        String externalPaymentId = event.externalPaymentId();

        RedisLockContext lockContext = acquireRedisLock(webhookId);

        try {
            Optional<Payment> paymentOpt = paymentJpaAdapter.findByExternalPaymentId(externalPaymentId);
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

    private RedisLockContext acquireRedisLock(String webhookId) {
        try {
            boolean acquired = idempotencyService.tryAcquireLock(webhookId);
            return new RedisLockContext(webhookId, acquired);
        } catch (Exception e) {
            log.warn("Redis 락 획득 실패, DB 기반 멱등성으로 진행: webhookId={}", webhookId);
            return new RedisLockContext(webhookId, false);
        }
    }

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

    private boolean isAlreadyProcessed(Payment payment) {
        return payment.getStatus() != PaymentStatus.PENDING;
    }

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

    private String generateWebhookId(PaymentMethod paymentMethod, WebhookEvent event) {
        return String.format("%s:%s:%s",
                paymentMethod.name(),
                event.externalPaymentId(),
                event.eventType());
    }
}
