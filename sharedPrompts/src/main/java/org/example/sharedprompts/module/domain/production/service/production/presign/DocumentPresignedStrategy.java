package org.example.sharedprompts.module.domain.production.service.production.presign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 문서 파일용 Presigned URL 생성 전략
 * HTML, MARKDOWN, JSON, TEXT 등 문서 타입 지원
 */
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
               lower.contains("html") ||
               lower.contains("markdown");
    }

    @Override
    public String generatePresignedUrl(String bucket, String key, String contentType, Duration ttl) {
        log.debug("Generating presigned URL for document - bucket: {}, key: {}, contentType: {}", 
                bucket, key, contentType);
        // 문서는 다운로드용으로 생성 (preview가 아닌 download)
        return presignedUrlGenerator.generate(bucket, key, ttl);
    }
}

