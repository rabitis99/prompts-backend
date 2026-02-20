package org.example.sharedprompts.module.domain.production.service.artifact;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.factory.ProductionArtifactDetailEntityFactory;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;
import org.example.sharedprompts.module.domain.production.application.storage.StorageFacade;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;
import org.example.sharedprompts.module.dto.response.production.ImageArtifactDto;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImageArtifactHandler implements ArtifactHandler {

    private final ArtifactAccessService artifactAccessService;
    private final ObjectMapper objectMapper;
    private final StorageFacade storageFacade;
    
    private static final Pattern IMG_SRC_PATTERN = Pattern.compile(
            "<img[^>]+src\\s*=\\s*[\"']([^\"']+)[\"']", 
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public ArtifactType getSupportedType() {
        return ArtifactType.IMAGE;
    }

    @Override
    public ProductionArtifactDetailEntity createDetail(
            String s3Key
    ) {
        // S3에 업로드된 실제 key를 그대로 사용
        String fileName = ArtifactMetadataHelper.extractFileName(s3Key);
        String contentType;

        // 이미지 포맷 감지를 위해 파일의 시작 부분만 읽기 (최대 12바이트)
        // 전체 파일을 다운로드하지 않아 네트워크 I/O와 메모리 사용을 최소화합니다.
        try {
            // WebP 포맷 감지를 위해 최대 12바이트 필요
            byte[] fileHeader = storageFacade.downloadRange(s3Key, 0, 11);
            if (fileHeader != null && fileHeader.length > 0) {
                String detectedFormat = detectImageFormat(fileHeader);
                contentType = getContentTypeFromFormat(detectedFormat);
                
                log.info("Detected image format from file header - s3Key: {}, format: {}, contentType: {}, fileName: {}", 
                        s3Key, detectedFormat, contentType, fileName);
            } else {
                // 파일을 읽을 수 없는 경우 파일명에서 추론
                log.warn("Could not read file header, inferring from s3Key - s3Key: {}", s3Key);
                contentType = ArtifactMetadataHelper.determineContentType(s3Key);
            }
        } catch (Exception e) {
            // 파일 읽기 실패 시 파일명에서 추론
            log.warn("Failed to read file header, inferring from s3Key - s3Key: {}, error: {}", s3Key, e.getMessage());
            contentType = ArtifactMetadataHelper.determineContentType(s3Key);
        }

        // S3에 업로드된 실제 key와 contentType을 그대로 사용하여 Entity 생성
        // ImageArtifactHandler는 항상 IMAGE 타입을 반환합니다.
        return ProductionArtifactDetailEntityFactory.createImage(
                s3Key,
                fileName,
                contentType,
                null // fileSize는 나중에 설정 가능
        );
    }

    /**
     * 파일 내용의 magic bytes를 확인하여 이미지 포맷을 감지합니다.
     */
    private String detectImageFormat(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 4) {
            return "jpg"; // 기본값
        }

        // PNG: 89 50 4E 47
        if (imageBytes[0] == (byte) 0x89 && imageBytes[1] == 0x50 
                && imageBytes[2] == 0x4E && imageBytes[3] == 0x47) {
            return "png";
        }
        
        // JPEG: FF D8 FF
        if (imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8 && imageBytes[2] == (byte) 0xFF) {
            return "jpg";
        }
        
        // GIF: 47 49 46 38 (GIF8)
        if (imageBytes.length >= 6 && imageBytes[0] == 0x47 && imageBytes[1] == 0x49 
                && imageBytes[2] == 0x46 && imageBytes[3] == 0x38) {
            return "gif";
        }
        
        // WebP: RIFF...WEBP
        if (imageBytes.length >= 12
                && imageBytes[0] == 0x52 && imageBytes[1] == 0x49 && imageBytes[2] == 0x46 && imageBytes[3] == 0x46
                && imageBytes[8] == 0x57 && imageBytes[9] == 0x45 && imageBytes[10] == 0x42 && imageBytes[11] == 0x50) {
            return "webp";
        }
        
        // BMP: 42 4D
        if (imageBytes[0] == 0x42 && imageBytes[1] == 0x4D) {
            return "bmp";
        }

        return "jpg"; // 기본값
    }

    /**
     * 포맷에 따라 content type을 반환합니다.
     */
    private String getContentTypeFromFormat(String format) {
        return switch (format.toLowerCase()) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            default -> "image/jpeg";
        };
    }


    @Override
    public ArtifactDto toDto(ProductionArtifactDetailEntity detail) {
        String s3Key = detail.getS3Key();
        String actualImagePath = s3Key;
        
        // HTML 파일인 경우 이미지 경로 추출
        // TODO: 성능 개선 - DTO 매핑 시점에 S3 I/O가 발생하면 응답 지연과 장애 전파 위험이 있습니다.
        //       이미지 경로를 엔티티 생성 시점(createDetail)에 미리 추출하여 별도 필드(예: actualImagePath)로
        //       저장하는 방식을 검토하면 좋겠습니다. 이렇게 하면 DTO 매핑은 메모리 기반 연산만 수행하게 됩니다.
        if (detail.getContentType() != null && detail.getContentType().contains("html")) {
            actualImagePath = extractImagePathFromHtml(s3Key);
            if (actualImagePath == null) {
                log.warn("Could not extract image path from HTML file - s3Key: {}", s3Key);
                actualImagePath = s3Key; // fallback to original path
            }
        }
        
        String cdnUrl = artifactAccessService.generateCdnUrl(actualImagePath);
        Map<String, String> thumbnailUrls = buildThumbnailUrls(detail.getMetadata());

        return new ImageArtifactDto(
                ArtifactType.IMAGE,
                actualImagePath, // 실제 이미지 경로 사용
                detail.getFileName(),
                detail.getContentType(),
                "S3", // storageLocation은 항상 S3
                thumbnailUrls,
                cdnUrl
        );
    }
    
    /**
     * HTML 파일에서 이미지 경로를 추출합니다.
     * 
     * 주의: 이 메서드는 S3에서 전체 HTML 파일을 다운로드하므로 I/O 비용이 발생합니다.
     * DTO 매핑 시점에 호출되면 응답 지연과 장애 전파 위험이 있으므로,
     * 향후 엔티티 생성 시점에 이미지 경로를 미리 추출하여 저장하는 방식으로 개선을 고려해야 합니다.
     * 
     * @param htmlFilePath HTML 파일의 S3 경로
     * @return 추출된 이미지 파일 경로, 추출 실패 시 null
     */
    private String extractImagePathFromHtml(String htmlFilePath) {
        try {
            byte[] htmlContent = storageFacade.download(htmlFilePath);
            String html = new String(htmlContent, StandardCharsets.UTF_8);
            
            Matcher matcher = IMG_SRC_PATTERN.matcher(html);
            if (matcher.find()) {
                String imagePath = matcher.group(1);
                log.debug("Extracted image path from HTML - htmlPath: {}, imagePath: {}", htmlFilePath, imagePath);
                return imagePath;
            }
            
            log.warn("No image path found in HTML file - filePath: {}", htmlFilePath);
            return null;
        } catch (Exception e) {
            log.error("Failed to extract image path from HTML - filePath: {}", htmlFilePath, e);
            return null;
        }
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
