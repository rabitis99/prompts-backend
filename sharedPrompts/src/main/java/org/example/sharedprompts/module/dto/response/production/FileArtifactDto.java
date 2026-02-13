package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

public record FileArtifactDto(
    @JsonProperty("type")
    ArtifactType type,

    @JsonIgnore
    String filePath,

    @JsonProperty("download_url")
    String downloadUrl,

    @JsonProperty("file_name")
    String fileName,

    @JsonProperty("content_type")
    String contentType,

    @JsonProperty("storage_location")
    String storageLocation,

    @JsonProperty("cdn_url")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String cdnUrl
) implements FileBasedArtifactDto, ArtifactDto {

    /**
     * 하위 호환 생성자 - cdnUrl 없이 생성
     */
    public FileArtifactDto(ArtifactType type, String filePath, String downloadUrl,
                           String fileName, String contentType, String storageLocation) {
        this(type, filePath, downloadUrl, fileName, contentType, storageLocation, null);
    }
}
