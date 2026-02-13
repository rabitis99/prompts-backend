package org.example.sharedprompts.module.domain.production.entity.production;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "production_artifact_details")
public class ProductionArtifactDetailEntity {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    @Setter(AccessLevel.PACKAGE)
    private ProductionArtifactEntity artifact;

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

    public void updateMetadata(String metadata) {
        this.metadata = metadata;
    }
}

