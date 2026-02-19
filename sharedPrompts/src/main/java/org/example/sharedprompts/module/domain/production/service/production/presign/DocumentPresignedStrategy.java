package org.example.sharedprompts.module.domain.production.service.production.presign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentPresignedStrategy implements PresignedStrategy {

    private final PresignedUrlGenerator presignedUrlGenerator;

    @Override
    public boolean supports(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lower = contentType.toLowerCase();
        return lower.startsWith("text/") ||
                lower.equals("application/json") ||
                lower.equals("application/xml") ||
                lower.equals("application/xhtml+xml") ||
                lower.contains("markdown");
    }

    @Override
    public String generatePresignedUrl(String bucket, String key, String contentType, Duration ttl) {
        log.debug("Generating presigned URL for document - bucket: {}, key: {}, contentType: {}", 
                bucket, key, contentType);
        String fileName = extractFileName(key);
        String contentDisposition = buildContentDisposition(fileName);
        return presignedUrlGenerator.generate(bucket, key, ttl, contentDisposition);
    }

    private String extractFileName(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        int lastSlashIndex = key.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < key.length() - 1) {
            return key.substring(lastSlashIndex + 1);
        }
        return key;
    }

    /**
     * RFC 6266 준수 Content-Disposition 헤더를 안전하게 생성합니다.
     * 파일명의 위험한 문자를 제거하고 UTF-8 인코딩을 지원합니다.
     *
     * @param fileName 파일명 (null 가능)
     * @return 안전한 Content-Disposition 헤더 값
     */
    private String buildContentDisposition(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "attachment";
        }

        // 위험한 문자 제거 (따옴표, 줄바꿈, 캐리지 리턴)
        String sanitized = fileName.replaceAll("[\"\\r\\n]", "_");

        // RFC 6266 준수: filename과 filename* 모두 제공
        // filename: ASCII-safe 버전 (호환성)
        // filename*: UTF-8 인코딩 버전 (비-ASCII 문자 지원)
        // URLEncoder.encode(String, Charset)는 Java 10+에서 checked exception을 던지지 않습니다.
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20"); // URLEncoder는 공백을 +로 인코딩하지만 RFC 6266은 %20을 선호
        return String.format("attachment; filename=\"%s\"; filename*=UTF-8''%s", sanitized, encoded);
    }
}

