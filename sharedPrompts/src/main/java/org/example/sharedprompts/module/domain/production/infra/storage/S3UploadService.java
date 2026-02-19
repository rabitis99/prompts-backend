package org.example.sharedprompts.module.domain.production.infra.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.ContentTypeUtils;
import org.example.sharedprompts.module.domain.production.infra.storage.exception.S3StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;

/**
 * S3 업로드 서비스
 * 파일 업로드를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3UploadService {

    private final S3Client s3Client;
    private final S3KeyGenerator keyGenerator;

    @Value("${production.storage.s3.bucket}")
    private String bucket;

    /**
     * 문자열 콘텐츠를 S3에 업로드합니다.
     */
    public String upload(String content, String tenantId, Long userId, String jobId, String fileName) {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        String contentType = ContentTypeUtils.guessContentType(fileName);
        return upload(data, contentType, tenantId, userId, jobId, fileName);
    }

    /**
     * 바이너리 데이터를 S3에 업로드합니다.
     */
    public String upload(byte[] data, String contentType, String tenantId, Long userId, String jobId, String fileName) {
        String correctedFileName = ensureCorrectExtension(fileName, contentType);
        String s3Key = keyGenerator.generateKey(tenantId, userId, jobId, correctedFileName);

        log.info("Uploading to S3 - bucket: {}, key: {}, contentType: {}, size: {} bytes, originalFileName: {}, correctedFileName: {}",
                bucket, s3Key, contentType, data.length, fileName, correctedFileName);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(contentType)
                    .contentLength((long) data.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(data));
            log.info("S3 upload completed - key: {}", s3Key);
            return s3Key;

        } catch (Exception e) {
            log.error("S3 upload failed - bucket: {}, key: {}", bucket, s3Key, e);
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
        
        if (lowerContentType.startsWith("image/")) {
            return switch (lowerContentType) {
                case "image/png" -> "png";
                case "image/jpeg", "image/jpg" -> "jpg";
                case "image/gif" -> "gif";
                case "image/webp" -> "webp";
                case "image/bmp" -> "bmp";
                case "image/svg+xml" -> "svg";
                default -> "jpg";
            };
        }
        
        if (lowerContentType.equals("application/pdf")) {
            return "pdf";
        }
        if (lowerContentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return "xlsx";
        }
        if (lowerContentType.equals("application/vnd.ms-excel")) {
            return "xls";
        }
        if (lowerContentType.equals("text/csv")) {
            return "csv";
        }
        if (lowerContentType.equals("text/html")) {
            return "html";
        }
        if (lowerContentType.equals("text/markdown")) {
            return "md";
        }
        if (lowerContentType.equals("application/json")) {
            return "json";
        }
        if (lowerContentType.equals("text/plain")) {
            return "txt";
        }

        return null;
    }
}

