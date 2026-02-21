package org.example.sharedprompts.module.domain.production.infra.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.ContentTypeUtils;
import org.example.sharedprompts.module.domain.production.config.condition.ConditionalOnStorageType;
import org.example.sharedprompts.module.domain.production.config.properties.ProductionS3Properties;
import org.example.sharedprompts.module.domain.production.infra.storage.exception.S3StorageException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * S3 업로드 서비스
 * 파일 업로드를 담당합니다.
 * production.storage.type=S3 일 때만 빈 등록되며, 버킷은 ProductionS3Properties @NotBlank로 검증됩니다.
 */
@Service
@ConditionalOnStorageType("S3")
@RequiredArgsConstructor
@Slf4j
public class S3UploadService {

    private final S3Client s3Client;
    private final S3KeyGenerator keyGenerator;
    private final ProductionS3Properties productionS3Properties;

    private String bucket() {
        return productionS3Properties.getBucket();
    }

    /**
     * Content-Type에서 파일 확장자로의 매핑
     */
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.ofEntries(
            Map.entry("image/png", "png"),
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/jpg", "jpg"),
            Map.entry("image/gif", "gif"),
            Map.entry("image/webp", "webp"),
            Map.entry("image/bmp", "bmp"),
            Map.entry("image/svg+xml", "svg"),
            Map.entry("application/pdf", "pdf"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
            Map.entry("application/vnd.ms-excel", "xls"),
            Map.entry("text/csv", "csv"),
            Map.entry("text/html", "html"),
            Map.entry("text/markdown", "md"),
            Map.entry("application/json", "json"),
            Map.entry("text/plain", "txt")
    );

    /**
     * 문자열 콘텐츠를 S3에 업로드합니다.
     */
    public String upload(String content, String tenantId, Long userId, String jobId, String fileName) {
        if (content == null) {
            throw new S3StorageException("Upload content must not be null");
        }
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        String contentType = ContentTypeUtils.guessContentType(fileName);
        return upload(data, contentType, tenantId, userId, jobId, fileName);
    }

    /**
     * 바이너리 데이터를 S3에 업로드합니다.
     */
    public String upload(byte[] data, String contentType, String tenantId, Long userId, String jobId, String fileName) {
        if (data == null) {
            throw new S3StorageException("Upload data must not be null");
        }
        String correctedFileName = ensureCorrectExtension(fileName, contentType);
        String s3Key = keyGenerator.generateKey(tenantId, userId, jobId, correctedFileName);

        log.info("Uploading to S3 - bucket: {}, key: {}, contentType: {}, size: {} bytes, originalFileName: {}, correctedFileName: {}",
                bucket(), s3Key, contentType, data.length, fileName, correctedFileName);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket())
                    .key(s3Key)
                    .contentType(contentType)
                    .contentLength((long) data.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(data));
            log.info("S3 upload completed - key: {}", s3Key);
            return s3Key;

        } catch (Exception e) {
            log.error("S3 upload failed - bucket: {}, key: {}", bucket(), s3Key, e);
            throw new S3StorageException("S3 upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * contentType을 기반으로 올바른 확장자를 가진 파일명을 생성합니다.
     */
    private String ensureCorrectExtension(String fileName, String contentType) {
        if (fileName == null || fileName.isBlank()) {
            fileName = "output";
        }

        String correctExtension = getExtensionFromContentType(contentType);
        
        int lastDot = fileName.lastIndexOf('.');
        String nameWithoutExt = lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
        String currentExt = lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";

        if (correctExtension != null && !currentExt.equals(correctExtension)) {
            return nameWithoutExt + "." + correctExtension;
        }

        return fileName;
    }

    /**
     * contentType에서 적절한 파일 확장자를 반환합니다.
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }

        String lowerContentType = contentType.toLowerCase();
        String extension = CONTENT_TYPE_TO_EXTENSION.get(lowerContentType);
        
        if (extension != null) {
            return extension;
        }
        
        // 알려지지 않은 image 타입에 대한 기본값
        if (lowerContentType.startsWith("image/")) {
            return "jpg";
        }

        return null;
    }

    /**
     * S3에서 파일을 삭제합니다.
     */
    public void delete(String s3Key) {
        log.info("Deleting from S3 - bucket: {}, key: {}", bucket(), s3Key);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket())
                    .key(s3Key)
                    .build());
            log.info("S3 delete completed - key: {}", s3Key);
        } catch (Exception e) {
            log.error("S3 delete failed - bucket: {}, key: {}", bucket(), s3Key, e);
            throw new S3StorageException("S3 delete failed: " + e.getMessage(), e);
        }
    }
}

