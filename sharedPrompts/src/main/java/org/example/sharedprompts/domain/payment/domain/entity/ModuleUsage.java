package org.example.sharedprompts.domain.payment.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.payment.domain.enums.ModuleType;
import org.example.sharedprompts.global.entity.BaseEntity;
import org.example.sharedprompts.domain.user.User;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "module_usage",
        indexes = {
                @Index(name = "idx_module_usage_user_id_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_module_usage_user_module_created", columnList = "user_id, module_type, created_at")
        }
)
public class ModuleUsage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "module_type", nullable = false, length = 50)
    private ModuleType moduleType;
}
