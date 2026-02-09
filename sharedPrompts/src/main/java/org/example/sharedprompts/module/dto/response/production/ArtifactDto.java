package org.example.sharedprompts.module.dto.response.production;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.module.domain.production.api.artifact.ArtifactType;
import org.example.sharedprompts.module.domain.production.api.artifact.ProductionArtifact;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactDto {
    private ArtifactType type;
    private String location;
    
    public static ArtifactDto from(ProductionArtifact artifact) {
        return ArtifactDto.builder()
                .type(artifact.getType())
                .location(artifact.getLocation())
                .build();
    }
}

