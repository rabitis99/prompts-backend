package org.example.sharedprompts.module.domain.production.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.module.domain.production.api.artifact.ArtifactType;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;
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
        @Index(name = "idx_production_artifacts_user_id_created_at", columnList = "user_id,created_at"),
        @Index(name = "idx_production_artifacts_user_id_command_type_created_at", columnList = "user_id,command_type,created_at"),
        @Index(name = "idx_production_artifacts_user_id_artifact_type_created_at", columnList = "user_id,artifact_type,created_at")
    }
)
public class ProductionArtifactEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}

