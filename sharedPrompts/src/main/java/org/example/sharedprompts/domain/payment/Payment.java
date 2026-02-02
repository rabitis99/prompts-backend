package org.example.sharedprompts.domain.payment;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.enums.PaymentUserType;
import org.example.sharedprompts.domain.payment.enums.UserTier;
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

    @Column(columnDefinition = "TEXT")
    private String metadata; // 추가 메타데이터 (JSON 형태)

    @Column
    private LocalDateTime nextRetryAt;
    
    @Column(length = 200)
    private String idempotencyKey; // 멱등성 키 (중복 호출 방지)

    /**
     * 결제 대기 중 상태로 변경
     */
    public void markPending() {
        this.status = PaymentStatus.PENDING;
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

    // ===== 상태 조회 메서드 (검증 제거, 조회만 유지) =====

    /**
     * 결제 성공 상태인지 확인
     *
     * <p>성공 여부만 조회 (비즈니스 규칙 검증 없음)
     */
    public boolean isSuccessful() {
        return this.status == PaymentStatus.SUCCESS
                || this.status == PaymentStatus.PARTIALLY_REFUNDED;
    }

    /**
     * 최종 상태인지 확인 (더 이상 상태 변경 불가)
     *
     * <p>상태 조회만 수행 (검증은 PaymentValidationService에서 담당)
     */
    public boolean isFinalState() {
        return this.status == PaymentStatus.FAILED
                || this.status == PaymentStatus.CANCELED
                || this.status == PaymentStatus.REFUNDED;
    }

    /**
     * 지수 백오프를 적용하여 다음 재시도 시간 예약
     * 오버플로우 방지를 위해 최대 지연 시간을 1시간으로 제한
     */
    public void scheduleNextRetry(long baseDelayMs) {
        long maxDelayMs = 3600000L; // 최대 1시간
        long delayMs = baseDelayMs * (long) Math.pow(2, this.retryCount);
        delayMs = Math.min(delayMs, maxDelayMs);
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
     * Webhook 결과 적용
     *
     * <p>Webhook에서 받은 PaymentResult를 기반으로 상태 변경
     *
     * @param externalPaymentId 외부 결제 ID
     * @param status 결제 상태
     * @param approvedAt 승인 시간 (선택)
     * @param failureReason 실패 사유 (선택, 외부 결제사에서 제공)
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

