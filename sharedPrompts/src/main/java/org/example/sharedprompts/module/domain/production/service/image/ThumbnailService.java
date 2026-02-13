package org.example.sharedprompts.module.domain.production.service.image;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactDetailRepository;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailService {

    private final ImageProcessor imageProcessor;
    private final StorageStrategy storageStrategy;
    private final ProductionArtifactDetailRepository detailRepository;
    private final ObjectMapper objectMapper;

    @Value("${thumbnail.sizes:200,400}")
    private List<Integer> thumbnailSizes;

    @Async("thumbnailTaskExecutor")
    @Transactional
    public void generateThumbnailsAsync(Long artifactDetailId, String originalStoragePath, String tenantId, Long userId, String jobId) {
        try {
            log.info("Starting thumbnail generation - detailId: {}, path: {}, tenantId: {}", artifactDetailId, originalStoragePath, tenantId);

            byte[] originalImage = storageStrategy.read(originalStoragePath);
            ImageMetadata metadata = imageProcessor.extractMetadata(originalImage);

            Map<String, String> thumbnailKeys = new HashMap<>();

            for (int size : thumbnailSizes) {
                try {
                    byte[] thumbnail = imageProcessor.generateThumbnail(originalImage, size);
                    String thumbnailFileName = "thumb_" + size + "_" + extractFileName(originalStoragePath);
                    String storedPath = storageStrategy.store(thumbnail, "image/jpeg", tenantId, userId, jobId, thumbnailFileName);
                    thumbnailKeys.put(String.valueOf(size), storedPath);
                    log.debug("Thumbnail generated - size: {}, path: {}", size, storedPath);
                } catch (Exception e) {
                    log.warn("Thumbnail generation failed for size {} - detailId: {}", size, artifactDetailId, e);
                }
            }

            updateArtifactMetadata(artifactDetailId, metadata, thumbnailKeys);
            log.info("Thumbnail generation completed - detailId: {}, thumbnails: {}", artifactDetailId, thumbnailKeys.size());

        } catch (Exception e) {
            log.error("Thumbnail generation failed - detailId: {}", artifactDetailId, e);
        }
    }

    private void updateArtifactMetadata(Long detailId, ImageMetadata metadata, Map<String, String> thumbnailKeys) {
        ProductionArtifactDetailEntity detail = detailRepository.findById(detailId).orElse(null);
        if (detail == null) {
            log.warn("Artifact detail not found for metadata update - detailId: {}", detailId);
            return;
        }

        try {
            Map<String, Object> metadataMap = (detail.getMetadata() != null && !detail.getMetadata().isBlank())
                    ? objectMapper.readValue(detail.getMetadata(), new TypeReference<Map<String, Object>>() {})
                    : new HashMap<>();

            metadataMap.put("width", metadata.width());
            metadataMap.put("height", metadata.height());
            metadataMap.put("fileSize", metadata.fileSize());
            metadataMap.put("format", metadata.format());
            metadataMap.put("thumbnails", thumbnailKeys);

            detail.updateMetadata(objectMapper.writeValueAsString(metadataMap));
            detailRepository.save(detail);
        } catch (Exception e) {
            log.error("Failed to update artifact metadata - detailId: {}", detailId, e);
        }
    }

    private String extractFileName(String path) {
        if (path == null) return "thumbnail.jpg";
        int lastSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSeparator >= 0 ? path.substring(lastSeparator + 1) : path;
    }
}
