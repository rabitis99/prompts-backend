package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

public record ImageArtifactDto(
    @JsonProperty("type")
    ArtifactType type,
    
    @JsonProperty("file_path")
    String filePath,
    
    @JsonProperty("preview_url")
    String previewUrl,
    
    @JsonProperty("file_name")
    String fileName,
    
    @JsonProperty("content_type")
    String contentType,
    
    @JsonProperty("storage_location")
    String storageLocation
) implements FileBasedArtifactDto, ArtifactDto {
}
