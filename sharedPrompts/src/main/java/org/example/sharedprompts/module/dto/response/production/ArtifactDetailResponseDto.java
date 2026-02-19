package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

import java.time.Instant;
import java.util.Map;

/**
 * Artifact 상세 정보 DTO
 * S3 Presigned URL을 포함합니다.
 * 
 * storageLocation은 항상 "S3"입니다.
 * presignedUrl은 다운로드/미리보기용 Presigned GET URL입니다.
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
    
    /**
     * 저장소 위치 (항상 "S3")
     */
    @JsonProperty("storage_location")
    String storageLocation,
    
    /**
     * S3 Presigned URL (다운로드/미리보기용)
     * 이미지의 경우 미리보기용, 파일의 경우 다운로드용으로 생성됩니다.
     */
    @JsonProperty("presigned_url")
    String presignedUrl,
    
    @JsonProperty("cdn_url")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String cdnUrl,
    
    @JsonProperty("thumbnail_urls")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Map<String, String> thumbnailUrls,
    
    @JsonProperty("created_at")
    Instant createdAt
) {
}

