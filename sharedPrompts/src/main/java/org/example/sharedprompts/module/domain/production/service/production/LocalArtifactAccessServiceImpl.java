package org.example.sharedprompts.module.domain.production.service.production;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * LOCAL 저장소 환경용 ArtifactAccessService 구현체.
 * Presigned URL이 필요 없으므로 파일 경로를 그대로 반환합니다.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "production.storage.type", havingValue = "LOCAL", matchIfMissing = true)
public class LocalArtifactAccessServiceImpl implements ArtifactAccessService {

    @Override
    public String generatePreviewUrl(String filePath) {
        log.debug("Local storage: returning file path as preview URL - {}", filePath);
        return filePath;
    }

    @Override
    public String generateDownloadUrl(String filePath) {
        log.debug("Local storage: returning file path as download URL - {}", filePath);
        return filePath;
    }
}

