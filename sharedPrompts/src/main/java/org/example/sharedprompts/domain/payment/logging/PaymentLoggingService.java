package org.example.sharedprompts.domain.payment.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 결제 관련 상세 로깅 및 추적 서비스
 * MDC를 사용한 트레이싱 ID 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentLoggingService {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String PAYMENT_ID_KEY = "paymentId";
    private static final String USER_ID_KEY = "userId";

    /**
     * 트레이싱 컨텍스트 시작
     */
    public String startTrace(Long paymentId, Long userId) {
        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID_KEY, traceId);
        MDC.put(PAYMENT_ID_KEY, String.valueOf(paymentId));
        MDC.put(USER_ID_KEY, String.valueOf(userId));
        
        log.info("결제 트레이싱 시작: traceId={}, paymentId={}, userId={}", traceId, paymentId, userId);
        return traceId;
    }

    /**
     * 트레이싱 컨텍스트 종료
     */
    public void endTrace() {
        String traceId = MDC.get(TRACE_ID_KEY);
        log.info("결제 트레이싱 종료: traceId={}", traceId);
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(PAYMENT_ID_KEY);
        MDC.remove(USER_ID_KEY);
    }

    /**
     * 결제 요청 로깅
     */
    public void logPaymentRequest(Payment payment) {
        log.info("결제 요청: paymentId={}, userId={}, amount={}, currency={}, paymentMethod={}, tier={}",
                payment.getId(),
                SensitiveDataMasker.maskUserId(payment.getUser().getId()),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getTier());
    }

    /**
     * 결제 승인 시도 로깅
     */
    public void logPaymentApprovalAttempt(Payment payment, String provider) {
        log.info("결제 승인 시도: paymentId={}, provider={}, externalPaymentId={}",
                payment.getId(),
                provider,
                payment.getExternalPaymentId() != null ? 
                    SensitiveDataMasker.maskString(payment.getExternalPaymentId(), 0, payment.getExternalPaymentId().length() - 4) : 
                    null);
    }

    /**
     * 결제 승인 성공 로깅
     */
    public void logPaymentApprovalSuccess(Payment payment, String externalPaymentId, long processingTimeMs) {
        log.info("결제 승인 성공: paymentId={}, externalPaymentId={}, processingTimeMs={}",
                payment.getId(),
                externalPaymentId != null ? 
                    SensitiveDataMasker.maskString(externalPaymentId, 0, externalPaymentId.length() - 4) : 
                    null,
                processingTimeMs);
    }

    /**
     * 결제 승인 실패 로깅
     */
    public void logPaymentApprovalFailure(Payment payment, String reason, Exception e) {
        log.error("결제 승인 실패: paymentId={}, reason={}, retryCount={}, error={}",
                payment.getId(),
                reason != null ? SensitiveDataMasker.maskSensitiveData(reason) : null,
                payment.getRetryCount(),
                e != null && e.getMessage() != null ? SensitiveDataMasker.maskSensitiveData(e.getMessage()) : null,
                e);
    }

    /**
     * 결제 상태 변경 로깅
     */
    public void logPaymentStatusChange(Payment payment, PaymentStatus oldStatus, PaymentStatus newStatus) {
        log.info("결제 상태 변경: paymentId={}, oldStatus={}, newStatus={}, externalPaymentId={}",
                payment.getId(),
                oldStatus,
                newStatus,
                payment.getExternalPaymentId() != null ? 
                    SensitiveDataMasker.maskString(payment.getExternalPaymentId(), 0, payment.getExternalPaymentId().length() - 4) : 
                    null);
    }

    /**
     * 결제 취소 로깅
     */
    public void logPaymentCancel(Payment payment, String reason) {
        log.info("결제 취소: paymentId={}, reason={}, externalPaymentId={}",
                payment.getId(),
                reason != null ? SensitiveDataMasker.maskSensitiveData(reason) : null,
                payment.getExternalPaymentId() != null ? 
                    SensitiveDataMasker.maskString(payment.getExternalPaymentId(), 0, payment.getExternalPaymentId().length() - 4) : 
                    null);
    }

    /**
     * 결제 환불 로깅
     */
    public void logPaymentRefund(Payment payment, BigDecimal refundAmount, String reason) {
        log.info("결제 환불: paymentId={}, refundAmount={}, reason={}, refundedAmount={}, refundableAmount={}",
                payment.getId(),
                refundAmount,
                reason,
                payment.getRefundedAmount(),
                payment.getRefundableAmount());
    }

    /**
     * 일일 결제 제한 체크 로깅
     */
    public void logDailyLimitCheck(Long userId, String tier, long todayCount, int limit) {
        log.debug("일일 결제 제한 체크: userId={}, tier={}, todayCount={}, limit={}",
                userId,
                tier,
                todayCount,
                limit);
    }

    /**
     * 일일 결제 제한 초과 로깅
     */
    public void logDailyLimitExceeded(Long userId, String tier, long todayCount, int limit) {
        log.warn("일일 결제 제한 초과: userId={}, tier={}, todayCount={}, limit={}",
                userId,
                tier,
                todayCount,
                limit);
    }

    /**
     * 재시도 로깅
     */
    public void logRetryAttempt(Payment payment, int attemptNumber) {
        log.info("결제 재시도: paymentId={}, attemptNumber={}, retryCount={}",
                payment.getId(),
                attemptNumber,
                payment.getRetryCount());
    }

    /**
     * Webhook 수신 로깅
     */
    public void logWebhookReceived(String provider, String eventType, String payload) {
        log.info("Webhook 수신: provider={}, eventType={}, payloadLength={}",
                provider,
                eventType,
                payload != null ? payload.length() : 0);
    }

    /**
     * Webhook 처리 실패 로깅
     */
    public void logWebhookProcessingFailure(String provider, String eventType, Exception e) {
        log.error("Webhook 처리 실패: provider={}, eventType={}, error={}",
                provider,
                eventType,
                e.getMessage(),
                e);
    }
}

