package org.example.sharedprompts.module.domain.production.service.artifact.mapper;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;
import org.example.sharedprompts.module.dto.response.production.FileArtifactDto;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileArtifactMapper implements ArtifactDtoMapper {
    
    private final ArtifactAccessService artifactAccessService;
    
    private static final String STORAGE_LOCATION_S3 = "S3";
    
    @Override
    public ArtifactType getSupportedType() {
        return ArtifactType.FILE;
    }
    
    @Override
    public ArtifactDto toDto(ProductionArtifactDetailEntity detail) {
        String s3Key = detail.getS3Key();
        String cdnUrl = artifactAccessService.generateCdnUrl(s3Key);
        
        return new FileArtifactDto(
                ArtifactType.FILE,
                s3Key,
                detail.getFileName(),
                detail.getContentType(),
                STORAGE_LOCATION_S3,
                cdnUrl
        );
    }
}

