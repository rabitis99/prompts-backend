package org.example.sharedprompts.module.domain.production.service.artifact;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategy;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;

public interface ArtifactHandler {

    ArtifactType getSupportedType();

    ProductionArtifactDetailEntity createDetail(
            JobEntity job,
            String filePath,
            StorageStrategy storageStrategy
    );

    ArtifactDto toDto(ProductionArtifactDetailEntity detail);
}
