package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

/**
 * Artifact 요약 정보 DTO
 * presigned URL은 포함하지 않고, artifactId와 type만 반환
 */
public record ArtifactSummaryDto(
    @JsonProperty("artifact_id")
    Long artifactId,
    
    @JsonProperty("type")
    ArtifactType type,
    
    @JsonProperty("is_primary")
    Boolean isPrimary,
    
    @JsonProperty("file_name")
    String fileName,
    
    @JsonProperty("content_type")
    String contentType
) {
}

