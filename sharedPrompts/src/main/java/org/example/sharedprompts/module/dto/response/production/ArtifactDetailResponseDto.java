package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

import java.time.Instant;
import java.util.Map;

/**
 * Artifact 상세 정보 DTO
 * presigned URL 포함
 */
public record ArtifactDetailResponseDto(
    @JsonProperty("artifact_id")
    Long artifactId,
    
    @JsonProperty("production_id")
    Long productionId,
    
    @JsonProperty("type")
    ArtifactType type,
    
    @JsonProperty("is_primary")
    Boolean isPrimary,
    
    @JsonProperty("file_name")
    String fileName,
    
    @JsonProperty("content_type")
    String contentType,
    
    @JsonProperty("storage_location")
    String storageLocation,
    
    @JsonProperty("presigned_url")
    String presignedUrl,
    
    @JsonProperty("cdn_url")
    String cdnUrl,
    
    @JsonProperty("thumbnail_urls")
    Map<String, String> thumbnailUrls,
    
    @JsonProperty("created_at")
    Instant createdAt
) {
}

