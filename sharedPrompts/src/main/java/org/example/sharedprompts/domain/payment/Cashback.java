package org.example.sharedprompts.domain.payment;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;
import org.example.sharedprompts.domain.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 캐시백 적립 내역 엔티티
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "cashbacks",
        indexes = {
                @Index(name = "idx_cashbacks_user_id_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_cashbacks_payment_id", columnList = "payment_id")
        }
)
public class Cashback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long paymentId; // 결제와 연관

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // 캐시백 금액

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal rate; // 캐시백 비율 (예: 0.05 = 5%)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal paymentAmount; // 결제 금액

    @Column(length = 200)
    private String description; // 캐시백 사유

    @Column(nullable = false)
    @Builder.Default
    private boolean paid = false; // 지급 완료 여부

    @Column
    private LocalDateTime paidAt; // 지급 시간

    /**
     * 캐시백 지급 처리
     */
    public void markAsPaid() {
        if (paid) {
            return;
        }
        this.paid = true;
        this.paidAt = LocalDateTime.now();
    }
}

