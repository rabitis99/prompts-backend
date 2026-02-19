package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 파일 기반 Artifact를 위한 인터페이스.
 * FILE과 IMAGE 타입이 구현하며, 파일 메타데이터 필드를 제공합니다.
 * 
 * S3 단일 저장 전략 사용으로 storageLocation은 항상 "S3"입니다.
 */
public sealed interface FileBasedArtifactDto extends ArtifactDto permits FileArtifactDto, ImageArtifactDto {
    
    @JsonProperty("file_name")
    String fileName();
    
    @JsonProperty("content_type")
    String contentType();
    
    /**
     * 저장소 위치 (항상 "S3")
     */
    @JsonProperty("storage_location")
    String storageLocation();
}

