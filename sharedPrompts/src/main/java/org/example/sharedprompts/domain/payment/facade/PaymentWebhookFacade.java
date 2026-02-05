package org.example.sharedprompts.domain.payment.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.provider.PaymentProviderFactory;
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

    private final PaymentProviderFactory providerFactory;
    private final PaymentRepository paymentRepository;
    private final PaymentWebhookTransactionService transactionService;
    private final WebhookIdempotencyService idempotencyService;
    private final DistributedLockService distributedLockService;

    /**
     * Webhook 처리 (메인 진입점)
     *
     * <p>처리 흐름:
     * <ol>
     *   <li>Webhook 파싱 및 서명 검증</li>
     *   <li>Redis 락 획득 시도 (실패해도 진행)</li>
     *   <li>DB에서 Payment 상태 확인 (멱등성)</li>
     *   <li>트랜잭션 내에서 상태 변경 및 저장</li>
     *   <li>Redis 처리 완료 마킹</li>
     * </ol>
     *
     * @param paymentMethod 결제 수단
     * @param payload       Webhook 페이로드
     * @param signature     서명 (검증용)
     * @return 처리된 Payment (이미 처리된 경우 기존 Payment 반환)
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
        // 1. Webhook 파싱 및 서명 검증
        PaymentProvider provider = providerFactory.getProvider(paymentMethod);
        PaymentProvider.WebhookEvent event = parseAndVerifyWebhook(provider, payload, signature, headers, paymentMethod);

        String webhookId = generateWebhookId(paymentMethod, event);
        String externalPaymentId = event.externalPaymentId();

        // 2. Redis 락 획득 시도 (실패해도 DB 기반 멱등성으로 진행)
        boolean lockAcquired = tryAcquireRedisLock(webhookId);

        try {
            // 3. DB에서 Payment 조회
            Optional<Payment> paymentOpt = paymentRepository.findByExternalPaymentId(externalPaymentId);
            if (paymentOpt.isEmpty()) {
                log.warn("Webhook 수신했으나 Payment를 찾을 수 없음: externalPaymentId={}", externalPaymentId);
                releaseRedisLockSafely(webhookId, lockAcquired, externalPaymentId, null);
                return Optional.empty();
            }

            Payment payment = paymentOpt.get();

            // 4. DB 기반 멱등성 검사: 이미 처리 완료된 상태면 바로 반환
            if (isAlreadyProcessed(payment)) {
                log.info("Payment 이미 처리 완료: paymentId={}, status={}, externalPaymentId={}",
                        payment.getId(), payment.getStatus(), externalPaymentId);
                releaseRedisLockSafely(webhookId, lockAcquired, externalPaymentId, null);
                return Optional.of(payment);
            }

            // 5. Payment 단위 분산 락으로 상태 변경 직렬화 (confirm/cancel/refund/webhook 간 충돌 방지)
            String lockKey = distributedLockService.createLockKey("payment", payment.getId()) + ":state";
            Payment processedPayment = distributedLockService.executeWithLock(lockKey,
                    () -> transactionService.processPaymentInTransaction(payment, event));

            // 6. DB 저장 성공 후 Redis에 처리 완료 마킹
            markRedisAsProcessed(webhookId, lockAcquired);

            return Optional.of(processedPayment);

        } catch (Exception e) {
            // 7. 예외 발생 시 Redis 락 해제
            releaseRedisLockSafely(webhookId, lockAcquired, externalPaymentId, e);
            throw e;
        }
    }

    /**
     * Webhook 파싱 및 서명 검증
     * 
     * <p>파싱 실패 시 400 Bad Request 반환하여 결제사에 재시도 중단 요청
     */
    private PaymentProvider.WebhookEvent parseAndVerifyWebhook(
            PaymentProvider provider,
            String payload,
            String signature,
            java.util.Map<String, String> headers,
            PaymentMethod paymentMethod
    ) {
        boolean verified = false;
        if (headers != null && !headers.isEmpty()) {
            verified = provider.verifyWebhookSignature(payload, headers);
        } else {
            verified = provider.verifyWebhookSignature(payload, signature);
        }

        if (!verified) {
            log.error("Webhook 서명 검증 실패: paymentMethod={}", paymentMethod);
            throw new ApiException(ErrorCode.PAYMENT_WEBHOOK_SIGNATURE_INVALID);
        }

        try {
            return provider.parseWebhook(payload);
        } catch (RuntimeException e) {
            // 파싱 실패(잘못된 형식 포함)는 복구 불가능한 오류로 간주하여 400 반환
            log.error("Webhook 파싱 실패: paymentMethod={}, error={}, payloadSize={}",
                    paymentMethod, e.getMessage(), payload != null ? payload.length() : 0);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_RESPONSE_INVALID,
                    "Webhook payload 파싱 실패: " + e.getMessage());
        }
    }

    /**
     * Redis 락 획득 시도 (실패해도 로그만 남기고 진행)
     */
    private boolean tryAcquireRedisLock(String webhookId) {
        try {
            boolean acquired = idempotencyService.tryAcquireLock(webhookId);
            if (!acquired) {
                log.debug("Redis 락 획득 실패, DB 기반 멱등성으로 진행: webhookId={}", webhookId);
            }
            return acquired;
        } catch (Exception e) {
            log.warn("Redis 락 획득 중 오류 발생, DB 기반 멱등성으로 진행: webhookId={}", webhookId, e);
            return false;
        }
    }

    /**
     * DB 기반 멱등성 검사: Payment 상태로 이미 처리되었는지 확인
     */
    private boolean isAlreadyProcessed(Payment payment) {
        PaymentStatus status = payment.getStatus();
        // PENDING이 아닌 상태는 이미 처리된 것으로 간주
        return status != PaymentStatus.PENDING;
    }


    /**
     * Redis에 처리 완료 마킹 (락을 획득한 경우에만)
     */
    private void markRedisAsProcessed(String webhookId, boolean lockAcquired) {
        if (!lockAcquired) {
            return;
        }
        try {
            idempotencyService.markAsProcessed(webhookId);
        } catch (Exception e) {
            // Redis 마킹 실패는 무시 (TTL로 자동 만료)
            log.warn("Redis 처리 완료 마킹 실패 (TTL로 자동 만료됨): webhookId={}", webhookId, e);
        }
    }

    /**
     * Redis 락 안전하게 해제
     */
    private void releaseRedisLockSafely(String webhookId, boolean lockAcquired,
                                         String externalPaymentId, Exception originalException) {
        if (!lockAcquired) {
            if (originalException != null) {
                log.error("Webhook 처리 중 오류 발생: webhookId={}, externalPaymentId={}",
                        webhookId, externalPaymentId, originalException);
            }
            return;
        }

        try {
            idempotencyService.releaseLock(webhookId);
            if (originalException != null) {
                log.error("Webhook 처리 중 오류 발생 (Redis 락 해제됨): webhookId={}, externalPaymentId={}",
                        webhookId, externalPaymentId, originalException);
            } else {
                log.debug("Redis 락 해제 완료: webhookId={}, externalPaymentId={}", webhookId, externalPaymentId);
            }
        } catch (Exception releaseException) {
            // Redis 락 해제 실패는 로그만 남김 (TTL로 자동 만료)
            if (originalException != null) {
                log.error("Webhook 처리 중 오류 발생 (Redis 락 해제 실패, TTL로 자동 만료됨): " +
                                "webhookId={}, externalPaymentId={}, releaseError={}",
                        webhookId, externalPaymentId, releaseException.getMessage(), originalException);
            } else {
                log.warn("Redis 락 해제 실패 (TTL로 자동 만료됨): webhookId={}, externalPaymentId={}, error={}",
                        webhookId, externalPaymentId, releaseException.getMessage());
            }
        }
    }

    /**
     * Webhook ID 생성 (멱등성 키)
     */
    private String generateWebhookId(PaymentMethod paymentMethod, PaymentProvider.WebhookEvent event) {
        return String.format("%s:%s:%s",
                paymentMethod.name(),
                event.externalPaymentId(),
                event.eventType());
    }
}
