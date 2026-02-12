package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextArtifactDto.class, name = "TEXT"),
    @JsonSubTypes.Type(value = FileArtifactDto.class, name = "FILE"),
    @JsonSubTypes.Type(value = ImageArtifactDto.class, name = "IMAGE")
})
public sealed interface ArtifactDto permits FileArtifactDto, FileBasedArtifactDto, ImageArtifactDto, TextArtifactDto {
    
    @JsonProperty("type")
    ArtifactType type();
}
