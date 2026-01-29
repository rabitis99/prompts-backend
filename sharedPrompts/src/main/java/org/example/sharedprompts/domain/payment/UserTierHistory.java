package org.example.sharedprompts.domain.payment;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;
import org.example.sharedprompts.domain.payment.enums.UserTier;
import org.example.sharedprompts.domain.user.User;

/**
 * 사용자 티어 변경 이력 엔티티
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "user_tier_history",
        indexes = {
                @Index(name = "idx_user_tier_history_user_id_created_at", columnList = "user_id, created_at")
        }
)
public class UserTierHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserTier previousTier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserTier newTier;

    @Column(nullable = true)
    private Long changedBy; // 변경한 사용자 ID (관리자 등)

    @Column(length = 200)
    private String reason; // 변경 사유
}

