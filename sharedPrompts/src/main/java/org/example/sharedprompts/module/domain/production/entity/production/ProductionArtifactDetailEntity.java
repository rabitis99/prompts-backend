package org.example.sharedprompts.module.domain.production.entity.production;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "production_artifact_details")
public class ProductionArtifactDetailEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_id", nullable = false)
    @Setter(AccessLevel.PACKAGE)
    private ProductionArtifactEntity artifact;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    public Boolean getIsPrimary() {
        return isPrimary != null ? isPrimary : false;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary != null ? isPrimary : false;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", length = 50)
    private ArtifactType artifactType;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", length = 50)
    private StorageFormat storageType;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "storage_location", length = 20)
    private String storageLocation;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public void updateMetadata(String metadata) {
        this.metadata = metadata;
    }
}

