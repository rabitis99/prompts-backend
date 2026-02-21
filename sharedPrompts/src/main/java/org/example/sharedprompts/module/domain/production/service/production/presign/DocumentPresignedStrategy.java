package org.example.sharedprompts.module.domain.production.service.production.presign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.infra.storage.S3PresignedUrlService;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.example.sharedprompts.module.domain.production.util.ContentDispositionBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentPresignedStrategy implements PresignedStrategy {

    private final S3PresignedUrlService presignedUrlService;

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
        String fileName = ArtifactMetadataHelper.extractFileName(key);
        String contentDisposition = ContentDispositionBuilder.attachment(fileName);
        return presignedUrlService.generateDownloadUrl(bucket, key, ttl, contentDisposition);
    }
}

