package org.example.sharedprompts.module.domain.production.service.production;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactRepository;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandler;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandlerRegistry;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategy;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionArtifactService {

    private final ProductionArtifactRepository productionArtifactRepository;
    private final ArtifactHandlerRegistry artifactHandlerRegistry;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductionArtifactEntity createArtifact(
            JobEntity job,
            String filePath,
            StorageStrategy storageStrategy
    ) {
        ProductionCommandType commandType;
        try {
            commandType = ProductionCommandType.valueOf(job.getCommandType());
        } catch (IllegalArgumentException e) {
            log.error("Invalid command type: {}", job.getCommandType(), e);
            throw new IllegalArgumentException("Invalid command type: " + job.getCommandType(), e);
        }

        ArtifactType artifactType = ArtifactMetadataHelper.determineArtifactType(commandType);
        
        ArtifactHandler handler;
        try {
            handler = artifactHandlerRegistry.getHandler(artifactType);
        } catch (IllegalArgumentException e) {
            log.error("No ArtifactHandler found for type: {}", artifactType, e);
            throw new IllegalArgumentException("No ArtifactHandler found for type: " + artifactType, e);
        }

        Instant startedAt;
        if (job.getStartedAt() != null) {
            startedAt = job.getStartedAt();
        } else if (job.getCreatedAt() != null) {
            startedAt = job.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toInstant();
        } else {
            startedAt = Instant.now();
        }

        ProductionArtifactEntity artifact = ProductionArtifactEntity.builder()
                .userId(job.getUserId())
                .commandType(commandType)
                .startedAt(startedAt)
                .completedAt(Instant.now())
                .success(true)
                .build();

        ProductionArtifactDetailEntity detail = handler.createDetail(filePath, storageStrategy);
        artifact.setDetail(detail);

        return productionArtifactRepository.save(artifact);
    }
}

