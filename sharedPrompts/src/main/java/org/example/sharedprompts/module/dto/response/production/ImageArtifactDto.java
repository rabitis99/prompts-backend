package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

import java.util.Map;

public record ImageArtifactDto(
    @JsonProperty("type")
    ArtifactType type,

    @JsonIgnore
    String filePath,

    @JsonProperty("preview_url")
    String previewUrl,

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

    /**
     * 하위 호환 생성자 - thumbnailUrls/cdnUrl 없이 생성
     */
    public ImageArtifactDto(ArtifactType type, String filePath, String previewUrl,
                            String fileName, String contentType, String storageLocation) {
        this(type, filePath, previewUrl, fileName, contentType, storageLocation, null, null);
    }
}
