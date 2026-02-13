package org.example.sharedprompts.module.domain.production.service.artifact;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;
import org.example.sharedprompts.module.dto.response.production.ImageArtifactDto;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImageArtifactHandler implements ArtifactHandler {

    private final ArtifactAccessService artifactAccessService;
    private final ObjectMapper objectMapper;

    @Override
    public ArtifactType getSupportedType() {
        return ArtifactType.IMAGE;
    }

    @Override
    public ArtifactDto toDto(ProductionArtifactDetailEntity detail) {
        String previewUrl = artifactAccessService.generatePreviewUrl(detail.getFilePath());
        String cdnUrl = artifactAccessService.generateCdnUrl(detail.getFilePath());
        Map<String, String> thumbnailUrls = buildThumbnailUrls(detail.getMetadata());

        return new ImageArtifactDto(
                ArtifactType.IMAGE,
                detail.getFilePath(),
                previewUrl,
                detail.getFileName(),
                detail.getContentType(),
                detail.getStorageLocation(),
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

            thumbnailKeys.forEach((size, key) ->
                    thumbnailUrls.put(size, artifactAccessService.generatePreviewUrl(key))
            );

            return thumbnailUrls.isEmpty() ? null : thumbnailUrls;
        } catch (Exception e) {
            log.warn("Failed to parse thumbnail metadata", e);
            return null;
        }
    }
}
