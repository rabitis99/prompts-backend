package org.example.sharedprompts.module.dto.response.production;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.module.domain.production.api.model.ProductionResult;

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
    
    public static ProductionResponseDto from(ProductionResult result, Long productionId) {
        return ProductionResponseDto.builder()
                .productionId(productionId)
                .success(result.isSuccess())
                .errorMessage(result.getErrorMessage())
                .startedAt(result.getStartedAt())
                .completedAt(result.getCompletedAt())
                .artifact(result.getArtifact() != null ? ArtifactDto.from(result.getArtifact()) : null)
                .build();
    }
}

