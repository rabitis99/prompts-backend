package org.example.sharedprompts.module.domain.production.service.production.presign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        String contentDisposition = fileName != null 
                ? String.format("attachment; filename=\"%s\"", fileName)
                : "attachment";
        return presignedUrlGenerator.generate(bucket, key, ttl, contentDisposition);
    }

    private String extractFileName(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        int lastSlash = key.lastIndexOf('/');
        return lastSlash >= 0 && lastSlash < key.length() - 1 
                ? key.substring(lastSlash + 1) 
                : key;
    }
}

