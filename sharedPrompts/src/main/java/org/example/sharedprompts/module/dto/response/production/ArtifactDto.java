package org.example.sharedprompts.module.dto.response.production;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.StorageFormat;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactDto {
    private ArtifactType type;
    private String location;
    private String fileName;
    private String contentType;
    private String storageLocation;

    public static ArtifactDto from(ProductionArtifactDetailEntity detail) {
        String location = detail.getStorageType() == StorageFormat.INLINE_TEXT
                ? detail.getContent()
                : detail.getFilePath();

        return ArtifactDto.builder()
                .type(detail.getArtifactType())
                .location(location)
                .fileName(detail.getFileName())
                .contentType(detail.getContentType())
                .storageLocation(detail.getStorageLocation())
                .build();
    }
}

