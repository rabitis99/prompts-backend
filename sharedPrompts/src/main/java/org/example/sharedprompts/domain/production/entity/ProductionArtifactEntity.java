package org.example.sharedprompts.domain.production.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.production.api.ArtifactType;
import org.example.sharedprompts.domain.production.api.ProductionCommandType;
import org.example.sharedprompts.global.entity.BaseEntity;

import java.time.Instant;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
    name = "production_artifacts",
    indexes = {
        @Index(name = "idx_production_artifacts_user_id", columnList = "user_id"),
        @Index(name = "idx_production_artifacts_production_id", columnList = "production_id"),
        @Index(name = "idx_production_artifacts_command_type", columnList = "command_type"),
        @Index(name = "idx_production_artifacts_created_at", columnList = "created_at")
    }
)
public class ProductionArtifactEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "production_id", nullable = false, length = 100)
    private String productionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false, length = 50)
    private ProductionCommandType commandType;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", length = 50)
    private ArtifactType artifactType;

    @Column(name = "location", columnDefinition = "TEXT")
    private String location;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}

