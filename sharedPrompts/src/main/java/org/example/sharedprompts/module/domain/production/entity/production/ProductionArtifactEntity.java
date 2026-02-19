package org.example.sharedprompts.module.domain.production.entity.production;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;

import java.time.Instant;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
    name = "production_artifacts",
    indexes = {
        @Index(name = "idx_pa_user_id_created_at", columnList = "user_id,created_at"),
        @Index(name = "idx_pa_user_id_command_type", columnList = "user_id,command_type,created_at"),
        @Index(name = "idx_pa_tenant_user", columnList = "tenant_id,user_id,created_at")
    }
)
public class ProductionArtifactEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false, length = 50)
    private ProductionCommandType commandType;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "success", nullable = false)
    private boolean success;

    @OneToMany(mappedBy = "artifact", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductionArtifactDetailEntity> artifacts = new java.util.ArrayList<>();

    public void addArtifact(ProductionArtifactDetailEntity artifact) {
        if (this.artifacts == null) {
            this.artifacts = new java.util.ArrayList<>();
        }
        this.artifacts.add(artifact);
        artifact.setArtifact(this);
    }

    @Deprecated
    public ProductionArtifactDetailEntity getDetail() {
        if (artifacts == null || artifacts.isEmpty()) {
            return null;
        }
        return artifacts.stream()
                .filter(ProductionArtifactDetailEntity::getIsPrimary)
                .findFirst()
                .orElse(artifacts.get(0));
    }

    @Deprecated
    public void setDetail(ProductionArtifactDetailEntity detail) {
        if (detail != null) {
            detail.setIsPrimary(true);
            addArtifact(detail);
        }
    }
}

