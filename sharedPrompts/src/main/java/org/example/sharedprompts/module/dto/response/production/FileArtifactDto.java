package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

/**
 * 파일 Artifact DTO
 * S3 저장소를 사용하며, storageLocation은 항상 "S3"입니다.
 * Presigned URL은 ArtifactDetailResponseDto의 presignedUrl 필드를 통해 제공됩니다.
 */
public record FileArtifactDto(
    @JsonProperty("type")
    ArtifactType type,

    @JsonIgnore
    String filePath,

    @JsonProperty("file_name")
    String fileName,

    @JsonProperty("content_type")
    String contentType,

    /**
     * 저장소 위치 (항상 "S3")
     */
    @JsonProperty("storage_location")
    String storageLocation,

    @JsonProperty("cdn_url")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String cdnUrl
) implements FileBasedArtifactDto, ArtifactDto {
}
