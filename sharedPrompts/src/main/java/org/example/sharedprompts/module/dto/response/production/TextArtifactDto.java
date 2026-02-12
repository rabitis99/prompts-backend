package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

/**
 * 텍스트 콘텐츠 Artifact DTO.
 * 파일 메타데이터 필드는 포함하지 않으며, 텍스트 콘텐츠만 다룹니다.
 */
public record TextArtifactDto(
    @JsonProperty("type")
    ArtifactType type,
    
    @JsonProperty("content")
    String content
) implements ArtifactDto {
}
