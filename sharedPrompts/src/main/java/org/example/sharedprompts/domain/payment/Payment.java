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

    /**
     * Mark the payment approved and record the external payment identifier.
     *
     * Sets the payment status to SUCCESS, stores the external payment provider's ID,
     * records the approval timestamp, and clears any recorded failure reason.
     *
     * @param externalPaymentId the identifier issued by the external payment provider
     */
    public void approve(String externalPaymentId) {
        this.status = PaymentStatus.SUCCESS;
        this.externalPaymentId = externalPaymentId;
        this.approvedAt = LocalDateTime.now();
        this.failureReason = null;
    }

    /**
     * Mark the payment as failed and record the failure reason.
     *
     * @param reason the human-readable reason for the failure to store on the payment
     */
    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    /**
     * Mark this payment as canceled.
     *
     * Sets the payment status to {@code CANCELED} and records the cancellation time.
     */
    public void cancel() {
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    /**
     * Apply a refund amount to the payment, updating the cumulative refunded amount and the payment status.
     *
     * @param refundAmount the amount to refund; added to the current refunded amount
     *                      — if the cumulative refunded amount is greater than or equal to the original payment amount,
     *                      the refunded amount is set to the original amount and the status becomes `REFUNDED`;
     *                      otherwise the refunded amount is updated to the cumulative total and the status becomes `PARTIALLY_REFUNDED`.
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
     * Increment the retry count for this payment by one.
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * Calculates the remaining amount that can be refunded.
     *
     * @return the refundable amount calculated as amount minus refundedAmount
     */
    public BigDecimal getRefundableAmount() {
        return this.amount.subtract(this.refundedAmount);
    }
}
