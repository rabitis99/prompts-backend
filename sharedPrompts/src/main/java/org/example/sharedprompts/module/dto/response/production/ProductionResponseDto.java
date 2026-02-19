package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ProductionResponseDto(
    @JsonProperty("production_id")
    Long productionId,
    
    @JsonProperty("status")
    ProductionStatus status,
    
    @JsonProperty("error_message")
    String errorMessage,
    
    @JsonProperty("started_at")
    Instant startedAt,
    
    @JsonProperty("completed_at")
    Instant completedAt,
    
    @JsonProperty("artifact")
    @Deprecated
    ArtifactDto artifact,
    
    @JsonProperty("artifacts")
    java.util.List<ArtifactSummaryDto> artifacts
) {
}
