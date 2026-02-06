package org.example.sharedprompts.domain.payment.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentUserType;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.domain.user.User;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 결제 엔티티
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_user_id_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_payments_status_created_at", columnList = "status, created_at"),
                @Index(name = "idx_payments_payment_method_created_at", columnList = "payment_method, created_at"),
                @Index(name = "idx_payments_external_id", columnList = "external_payment_id")
        }
)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency; // ISO 4217 통화 코드 (KRW, USD, EUR 등)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentUserType userType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserTier tier;

    @Column(length = 200)
    private String externalPaymentId; // 결제사에서 발급한 결제 ID

    @Column(columnDefinition = "TEXT")
    private String failureReason; // 실패 사유

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column
    private LocalDateTime approvedAt; // 승인 시간

    @Column
    private LocalDateTime canceledAt; // 취소 시간

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO; // 환불된 금액

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal usedPointAmount = BigDecimal.ZERO; // 사용한 포인트 금액

    @Column(precision = 19, scale = 2)
    private BigDecimal originalAmount; // 원본 결제 금액 (카카오페이 취소/환불 시 사용)

    @Column(precision = 19, scale = 2)
    private BigDecimal taxFreeAmount; // 면세 금액 (카카오페이 취소/환불 시 사용)

    @Column(precision = 19, scale = 6)
    private BigDecimal exchangeRate; // 환율 (requestPayment 시점의 환율 저장)

    @Column(length = 3)
    private String originalCurrency; // 원본 통화 코드 (환율 변환 전 통화)

    @Column(columnDefinition = "TEXT")
    private String metadata; // 추가 메타데이터 (JSON 형태)

    @Column
    private LocalDateTime nextRetryAt;
    
    @Column(length = 200)
    private String idempotencyKey; // 멱등성 키 (중복 호출 방지)

    @Version
    private Long version; // 낙관적 락을 위한 버전 필드

    /**
     * 결제 대기 중 상태로 변경
     */
    public void markPending() {
        this.status = PaymentStatus.PENDING;
    }

    /**
     * 결제 상태를 업데이트합니다.
     * 외부 결제사 상태 동기화 시 사용됩니다.
     */
    public void updateStatus(PaymentStatus status) {
        this.status = status;
    }

    /**
     * 결제 승인 처리 (성공)
     */
    public void approve(String externalPaymentId) {
        markSuccess(externalPaymentId);
    }

    /**
     * 결제 성공 처리
     */
    public void markSuccess(String externalPaymentId) {
        this.status = PaymentStatus.SUCCESS;
        this.externalPaymentId = externalPaymentId;
        this.approvedAt = LocalDateTime.now();
        this.failureReason = null;
    }

    /**
     * 결제 실패 처리
     */
    public void fail(String reason) {
        markFailed(reason);
    }

    /**
     * 결제 실패 처리
     */
    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        // userType과 tier는 변경하지 않음 (결제 실패 시 등급 상승 방지)
    }

    /**
     * 결제 취소 처리
     */
    public void cancel() {
        markCanceled();
    }

    /**
     * 결제 취소 처리
     */
    public void markCanceled() {
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    /**
     * 환불 처리
     */
    public void refund(BigDecimal refundAmount) {
        BigDecimal newRefundedAmount = this.refundedAmount.add(refundAmount);
        
        if (newRefundedAmount.compareTo(this.amount) >= 0) {
            // 전체 환불
            this.status = PaymentStatus.REFUNDED;
            this.refundedAmount = this.amount;
        } else {
            // 부분 환불
            this.status = PaymentStatus.PARTIALLY_REFUNDED;
            this.refundedAmount = newRefundedAmount;
        }
    }

    /**
     * 재시도 횟수 증가
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * 환불 가능한 금액 계산
     */
    public BigDecimal getRefundableAmount() {
        return this.amount.subtract(this.refundedAmount);
    }

    /**
     * 재시도 가능 여부 확인
     */
    public boolean isRetryable(int maxRetry) {
        return this.status == PaymentStatus.PENDING
                && this.retryCount < maxRetry;
    }

    /**
     * 복구되지 않은 포인트가 있는지 확인
     */
    public boolean hasUnrecoveredPoints() {
        return this.status == PaymentStatus.PENDING
                && this.usedPointAmount != null
                && this.usedPointAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 지수 백오프를 적용하여 다음 재시도 시간 예약
     * 오버플로우 방지를 위해 최대 지연 시간을 1시간으로 제한
     */
    public void scheduleNextRetry(long baseDelayMs) {
        long maxDelayMs = 3600000L; // 최대 1시간
        // 오버플로우 방지를 위해 지수 계산 결과를 먼저 제한
        double exponentialFactor = Math.min(Math.pow(2, this.retryCount), maxDelayMs / (double) baseDelayMs + 1);
        long delayMs = Math.min((long) (baseDelayMs * exponentialFactor), maxDelayMs);
        this.nextRetryAt = LocalDateTime.now().plus(Duration.ofMillis(delayMs));
    }
    
    /**
     * 멱등성 키 설정
     */
    public void updateIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
    
    /**
     * 외부 결제 ID 업데이트
     */
    public void updateExternalPaymentId(String externalPaymentId) {
        this.externalPaymentId = externalPaymentId;
    }

    /**
     * 메타데이터 업데이트
     */
    public void updateMetadata(String metadata) {
        this.metadata = metadata;
    }

    /**
     * 원본 결제 금액 및 면세 금액 저장 (카카오페이 취소/환불 시 사용)
     */
    public void updateOriginalAmounts(BigDecimal originalAmount, BigDecimal taxFreeAmount) {
        this.originalAmount = originalAmount;
        this.taxFreeAmount = taxFreeAmount;
    }

    /**
     * Webhook 결과 적용
     */
    public void applyWebhookResult(String externalPaymentId, PaymentStatus status, LocalDateTime approvedAt, String failureReason) {
        this.externalPaymentId = externalPaymentId;
        this.status = status;
        if (approvedAt != null) {
            this.approvedAt = approvedAt;
        }
        if (status == PaymentStatus.SUCCESS && this.approvedAt == null) {
            this.approvedAt = LocalDateTime.now();
        }
        if (status == PaymentStatus.SUCCESS) {
            this.failureReason = null; // 성공 시 failureReason 초기화
        } else if (status == PaymentStatus.FAILED) {
            this.failureReason = failureReason != null ? failureReason : "Webhook에서 결제 실패 확인";
        }
    }
}

