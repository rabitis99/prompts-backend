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
import java.util.List;
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
    
    private static final String STORAGE_LOCATION_S3 = "S3";

    @Override
    public ArtifactType getSupportedType() {
        return ArtifactType.IMAGE;
    }

    /**
     * P1-3: S3 I/O를 트랜잭션 밖에서 수행하기 위한 메서드
     * format 감지와 HTML 파싱을 먼저 수행하고, 결과를 파라미터로 전달하여 entity 생성
     */
    public ImageDetailData prepareDetailData(String s3Key) {
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

        // HTML 파일인 경우 이미지 경로를 미리 추출하여 저장
        // DTO 매핑 시점에 S3 I/O가 발생하지 않도록 엔티티 생성 시점에 처리합니다.
        String actualImagePath = null;
        if (contentType != null && contentType.contains("html")) {
            actualImagePath = extractImagePathFromHtml(s3Key);
            if (actualImagePath == null) {
                log.warn("Could not extract image path from HTML file during entity creation - s3Key: {}", s3Key);
                // 추출 실패 시 s3Key 사용 (Factory에서 null이면 s3Key로 설정됨)
            } else {
                log.info("Extracted image path from HTML during entity creation - s3Key: {}, actualImagePath: {}", 
                        s3Key, actualImagePath);
            }
        }

        return new ImageDetailData(s3Key, fileName, contentType, actualImagePath);
    }

    /**
     * P1-3: 준비된 데이터로부터 엔티티 생성 (트랜잭션 안에서 실행)
     */
    public ProductionArtifactDetailEntity createDetailFromData(ImageDetailData data) {
        return ProductionArtifactDetailEntityFactory.createImage(
                data.s3Key(),
                data.fileName(),
                data.contentType(),
                null, // fileSize는 나중에 설정 가능
                data.actualImagePath()
        );
    }

    @Override
    public ProductionArtifactDetailEntity createDetail(String s3Key) {
        // P1-3: 기존 메서드는 하위 호환성을 위해 유지하되, 내부적으로 prepareDetailData + createDetailFromData 사용
        // 하지만 이 메서드가 트랜잭션 안에서 호출되므로 S3 I/O가 트랜잭션 안에서 실행됨
        // 호출하는 쪽에서 prepareDetailData()를 먼저 호출하고 createDetailFromData()를 사용하도록 변경 권장
        ImageDetailData data = prepareDetailData(s3Key);
        return createDetailFromData(data);
    }

    /**
     * P1-3: S3 I/O 결과를 담는 데이터 클래스
     */
    public record ImageDetailData(
            String s3Key,
            String fileName,
            String contentType,
            String actualImagePath
    ) {}

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
        return new org.example.sharedprompts.module.domain.production.service.artifact.mapper.ImageArtifactMapper(
                artifactAccessService, objectMapper).toDto(detail);
    }
    
    /**
     * HTML 파일에서 이미지 경로를 추출합니다.
     * 
     * 이 메서드는 엔티티 생성 시점(createDetail)에만 호출되며,
     * 추출된 이미지 경로는 엔티티의 actualImagePath 필드에 저장됩니다.
     * DTO 매핑 시점에는 저장된 값을 사용하므로 S3 I/O가 발생하지 않습니다.
     * 
     * @param htmlFilePath HTML 파일의 S3 경로
     * @return 추출된 이미지 파일 경로, 추출 실패 시 null
     */
    private String extractImagePathFromHtml(String htmlFilePath) {
        try {
            // P1-3: S3 I/O (트랜잭션 안에서 실행됨)
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
            log.warn("Failed to parse thumbnail metadata", e);
            return null;
        }
    }
}
