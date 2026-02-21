package org.example.sharedprompts.module.domain.production.service.artifact.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;
import org.example.sharedprompts.module.dto.response.production.ImageArtifactDto;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ImageArtifactMapper implements ArtifactDtoMapper {
    
    private final ArtifactAccessService artifactAccessService;
    private final ObjectMapper objectMapper;
    
    private static final String STORAGE_LOCATION_S3 = "S3";
    
    @Override
    public ArtifactType getSupportedType() {
        return ArtifactType.IMAGE;
    }
    
    @Override
    public ArtifactDto toDto(ProductionArtifactDetailEntity detail) {
        String actualImagePath = detail.getActualImagePath();
        if (actualImagePath == null || actualImagePath.isBlank()) {
            actualImagePath = detail.getS3Key();
        }
        
        String cdnUrl = artifactAccessService.generateCdnUrl(actualImagePath);
        Map<String, String> thumbnailUrls = buildThumbnailUrls(detail.getMetadata());
        
        return new ImageArtifactDto(
                ArtifactType.IMAGE,
                actualImagePath,
                detail.getFileName(),
                detail.getContentType(),
                STORAGE_LOCATION_S3,
                thumbnailUrls,
                cdnUrl
        );
    }
    
    private Map<String, String> buildThumbnailUrls(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        
        try {
            Map<String, Object> metadata = objectMapper.readValue(metadataJson, new TypeReference<>() {});
            Object thumbnails = metadata.get("thumbnails");
            if (!(thumbnails instanceof Map)) {
                return null;
            }
            
            Map<String, String> thumbnailKeys = new HashMap<>();
            ((Map<?, ?>) thumbnails).forEach((k, v) -> {
                if (k instanceof String && v instanceof String) {
                    thumbnailKeys.put((String) k, (String) v);
                }
            });
            Map<String, String> thumbnailUrls = new HashMap<>();
            
            // P1-4: batch presign URL 생성으로 개선 - 여러 thumbnail에 대한 URL을 일괄 생성
            List<String> keys = thumbnailKeys.values().stream().toList();
            Map<String, String> batchUrls = artifactAccessService.generatePreviewUrls(keys);
            
            // size를 키로 하는 맵으로 변환
            thumbnailKeys.forEach((size, key) -> {
                String url = batchUrls.get(key);
                if (url != null) {
                    thumbnailUrls.put(size, url);
                }
            });
            
            return thumbnailUrls.isEmpty() ? null : thumbnailUrls;
        } catch (Exception e) {
            return null;
        }
    }
}

