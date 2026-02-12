package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

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
    String storageLocation
) implements FileBasedArtifactDto, ArtifactDto {
}
