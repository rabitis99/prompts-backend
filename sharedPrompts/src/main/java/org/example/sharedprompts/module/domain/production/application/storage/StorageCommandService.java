package org.example.sharedprompts.module.domain.production.application.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.infra.storage.S3KeyGenerator;
import org.example.sharedprompts.module.domain.production.infra.storage.S3PresignedUrlService;
import org.example.sharedprompts.module.domain.production.model.tenant.TenantContext;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactDetailRepository;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Storage Command Service
 * Presigned URL 생성 등 명령 작업을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageCommandService {

    private final S3PresignedUrlService presignedUrlService;
    private final S3KeyGenerator keyGenerator;
    private final ProductionArtifactDetailRepository artifactDetailRepository;

    /**
     * 업로드용 Presigned URL 생성
     * TTL은 S3PresignedUrlService의 기본값을 사용합니다.
     * 비동기 컨텍스트 등에서 tenantId가 null일 수 있습니다.
     */
    public String generateUploadPresignedUrl(Long userId, String jobId, String fileName, String contentType) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("TenantContext.getCurrentTenantId() returned null - userId: {}, jobId: {}, fileName: {}. " +
                    "Presigned URL will be generated without tenant prefix. Consider using tenant-aware method in async contexts.",
                    userId, jobId, fileName);
        }
        String s3Key = keyGenerator.generateKey(tenantId, userId, jobId, fileName);
        return presignedUrlService.generateUploadUrl(s3Key, contentType, null);
    }

    /**
     * 다운로드용 Presigned URL 생성 (보안 검증 포함)
     * 아티팩트 ID를 통해 소유권을 검증한 후 Presigned URL을 생성합니다.
     */
    @Transactional(readOnly = true)
    public String generateDownloadPresignedUrl(Long artifactId, Long userId) {
        String s3Key = resolveS3KeyWithOwnerCheck(artifactId, userId);
        // TTL은 S3PresignedUrlService의 기본값을 사용합니다.
        return presignedUrlService.generateDownloadUrl(s3Key, null);
    }

    /**
     * 미리보기용 Presigned URL 생성 (보안 검증 포함)
     * 아티팩트 ID를 통해 소유권을 검증한 후 Presigned URL을 생성합니다.
     */
    @Transactional(readOnly = true)
    public String generatePreviewPresignedUrl(Long artifactId, Long userId) {
        String s3Key = resolveS3KeyWithOwnerCheck(artifactId, userId);
        // TTL은 S3PresignedUrlService의 기본값을 사용합니다.
        return presignedUrlService.generatePreviewUrl(s3Key, null);
    }

    /**
     * 아티팩트 ID와 사용자 ID를 기반으로 소유권을 검증하고 S3 키를 반환합니다.
     *
     * @param artifactId 아티팩트 ID
     * @param userId 사용자 ID
     * @return 검증된 S3 키
     * @throws BaseException 아티팩트를 찾을 수 없거나 소유권이 없거나 파일 경로가 유효하지 않은 경우
     */
    @Transactional(readOnly = true)
    private String resolveS3KeyWithOwnerCheck(Long artifactId, Long userId) {
        ProductionArtifactDetailEntity artifact = artifactDetailRepository.findById(artifactId)
                .orElseThrow(() -> new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND));

        ProductionArtifactEntity production = artifact.getArtifact();
        if (production == null) {
            throw new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND);
        }

        if (!production.getUserId().equals(userId)) {
            throw new BaseException(ModuleErrorCode.PRODUCTION_FORBIDDEN);
        }

        String s3Key = artifact.getS3Key();
        if (s3Key == null || s3Key.isBlank()) {
            throw new BaseException(ModuleErrorCode.STORAGE_ERROR, null, "Artifact s3Key is not available");
        }

        // s3Key가 s3:// 형식일 수 있으므로 처리
        return extractS3Key(s3Key);
    }

    /**
     * 다운로드용 Presigned URL 생성 (레거시 - 보안 취약점 있음)
     * @deprecated 보안을 위해 generateDownloadPresignedUrl(Long artifactId, Long userId) 사용을 권장합니다.
     */
    @Deprecated
    public String generateDownloadPresignedUrl(String s3Key) {
        // TTL은 S3PresignedUrlService의 기본값을 사용합니다.
        return presignedUrlService.generateDownloadUrl(s3Key, null);
    }

    /**
     * 미리보기용 Presigned URL 생성 (레거시 - 보안 취약점 있음)
     * @deprecated 보안을 위해 generatePreviewPresignedUrl(Long artifactId, Long userId) 사용을 권장합니다.
     */
    @Deprecated
    public String generatePreviewPresignedUrl(String s3Key) {
        // TTL은 S3PresignedUrlService의 기본값을 사용합니다.
        return presignedUrlService.generatePreviewUrl(s3Key, null);
    }

    /**
     * filePath에서 S3 key를 추출합니다.
     */
    private String extractS3Key(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        if (filePath.startsWith("s3://")) {
            String withoutPrefix = filePath.substring(5);
            int slashIndex = withoutPrefix.indexOf('/');
            if (slashIndex > 0) {
                return withoutPrefix.substring(slashIndex + 1);
            }
        }
        return filePath;
    }
}

