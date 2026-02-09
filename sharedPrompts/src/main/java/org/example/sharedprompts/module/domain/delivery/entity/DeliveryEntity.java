package org.example.sharedprompts.module.domain.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.module.domain.delivery.api.type.DeliveryType;
import org.example.sharedprompts.global.entity.BaseEntity;

import java.time.Instant;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
    name = "deliveries",
    indexes = {
        @Index(name = "idx_deliveries_user_id_created_at", columnList = "user_id,created_at"),
        @Index(name = "idx_deliveries_user_id_delivery_type_created_at", columnList = "user_id,delivery_type,created_at"),
        @Index(name = "idx_deliveries_production_artifact_id", columnList = "production_artifact_id")
    }
)
public class DeliveryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_id", nullable = false, length = 100, unique = true)
    private String deliveryId;

    @Column(name = "production_artifact_id", nullable = false)
    private Long productionArtifactId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 50)
    private DeliveryType deliveryType;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}

