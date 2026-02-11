package org.example.sharedprompts.module.domain.production.service.production;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactRepository;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategy;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionArtifactService {

    private final ProductionArtifactRepository productionArtifactRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductionArtifactEntity createArtifact(
            JobEntity job,
            String filePath,
            StorageStrategy storageStrategy
    ) {
        ProductionCommandType commandType = ProductionCommandType.valueOf(job.getCommandType());

        var artifactType = ArtifactMetadataHelper.determineArtifactType(commandType);
        var storageFormat = ArtifactMetadataHelper.determineStorageFormat(filePath);
        String fileName = ArtifactMetadataHelper.extractFileName(filePath);
        String contentType = ArtifactMetadataHelper.determineContentType(filePath);
        String storageLocation = storageStrategy.getStorageType().name();

        ProductionArtifactEntity artifact = ProductionArtifactEntity.builder()
                .userId(job.getUserId())
                .commandType(commandType)
                .startedAt(job.getStartedAt() != null ? job.getStartedAt() : Instant.from(job.getCreatedAt()))
                .completedAt(Instant.now())
                .success(true)
                .build();

        ProductionArtifactDetailEntity detail = ProductionArtifactDetailEntity.builder()
                .artifactType(artifactType)
                .storageType(storageFormat)
                .filePath(filePath)
                .fileName(fileName)
                .contentType(contentType)
                .storageLocation(storageLocation)
                .build();

        artifact.setDetail(detail);

        return productionArtifactRepository.save(artifact);
    }
}

