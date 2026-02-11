package org.example.sharedprompts.module.dto.response.production;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionResponseDto {

    private Long productionId;
    private boolean success;
    private String errorMessage;
    private Instant startedAt;
    private Instant completedAt;
    private ArtifactDto artifact;

    public static ProductionResponseDto from(ProductionArtifactEntity entity) {
        return ProductionResponseDto.builder()
                .productionId(entity.getId())
                .success(entity.isSuccess())
                .errorMessage(entity.getDetail() != null ? entity.getDetail().getErrorMessage() : null)
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .artifact(entity.getDetail() != null ? ArtifactDto.from(entity.getDetail()) : null)
                .build();
    }
}

