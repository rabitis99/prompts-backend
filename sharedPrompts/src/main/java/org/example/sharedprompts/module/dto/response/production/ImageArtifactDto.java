package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

import java.util.Map;

/**
 * 이미지 Artifact DTO
 * S3 저장소를 사용하며, storageLocation은 항상 "S3"입니다.
 * Presigned URL은 ArtifactDetailResponseDto의 presignedUrl 필드를 통해 제공됩니다.
 */
public record ImageArtifactDto(
    @JsonProperty("type")
    ArtifactType type,

    @JsonIgnore
    String filePath,

    @JsonProperty("file_name")
    String fileName,

    @JsonProperty("content_type")
    String contentType,

    @JsonIgnore
    String storageLocation,

    @JsonProperty("thumbnail_urls")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Map<String, String> thumbnailUrls,

    @JsonProperty("cdn_url")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String cdnUrl
) implements FileBasedArtifactDto, ArtifactDto {
}
