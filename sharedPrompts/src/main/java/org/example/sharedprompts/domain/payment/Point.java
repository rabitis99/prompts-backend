package org.example.sharedprompts.domain.payment;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.payment.enums.PointType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 포인트 적립 내역 엔티티
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "points",
        indexes = {
                @Index(name = "idx_points_user_id_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_points_payment_id", columnList = "payment_id")
        }
)
public class Point extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = true)
    private Payment payment; // 결제와 연관된 경우 (결제 관련 포인트만 해당)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // 적립/사용 포인트 (양수: 적립, 음수: 사용)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PointType type; // 적립 타입 (PAYMENT, CASHBACK, PROMOTION, EVENT 등)

    @Column(length = 200)
    private String description; // 적립/사용 사유

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO; // 적립/사용 후 잔액

    @Column(nullable = false)
    @Builder.Default
    private boolean expired = false; // 만료 여부

    @Column
    private LocalDateTime expiredAt; // 만료 시간
}

