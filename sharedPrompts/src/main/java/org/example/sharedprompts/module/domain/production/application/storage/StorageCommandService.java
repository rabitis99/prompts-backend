package org.example.sharedprompts.module.domain.production.application.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.presign.PresignedUrlService;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.infra.storage.S3KeyGenerator;
import org.example.sharedprompts.module.domain.production.model.tenant.TenantContext;
import org.example.sharedprompts.module.domain.production.service.production.access.ArtifactOwnershipValidator;
import org.example.sharedprompts.module.domain.production.util.s3.S3PathUtils;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Service;

/**
 * Storage Command Service
 * Presigned URL 생성 등 명령 작업을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageCommandService {

    private final PresignedUrlService presignedUrlService;
    private final S3KeyGenerator keyGenerator;
    private final ArtifactOwnershipValidator ownershipValidator;

    /**
     * 업로드용 Presigned URL 생성
     * TTL은 PresignedUrlService의 기본값을 사용합니다.
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
        return presignedUrlService.generateUploadUrlByKey(s3Key, contentType, null);
    }

    /**
     * 다운로드용 Presigned URL 생성 (보안 검증 포함)
     * 아티팩트 ID를 통해 소유권을 검증한 후 Presigned URL을 생성합니다.
     */
    public String generateDownloadPresignedUrl(Long artifactId, Long userId) {
        String s3Key = resolveS3KeyWithOwnerCheck(artifactId, userId);
        // TTL은 PresignedUrlService의 기본값을 사용합니다.
        return presignedUrlService.generateDownloadUrlByKey(s3Key, null);
    }

    /**
     * 미리보기용 Presigned URL 생성 (보안 검증 포함)
     * 아티팩트 ID를 통해 소유권을 검증한 후 Presigned URL을 생성합니다.
     */
    public String generatePreviewPresignedUrl(Long artifactId, Long userId) {
        String s3Key = resolveS3KeyWithOwnerCheck(artifactId, userId);
        // TTL은 PresignedUrlService의 기본값을 사용합니다.
        return presignedUrlService.generatePreviewUrlByKey(s3Key, null);
    }

    /**
     * 아티팩트 소유권을 검증한 뒤 S3 키를 추출합니다.
     */
    private String resolveS3KeyWithOwnerCheck(Long artifactId, Long userId) {
        // Repository fetch join(EntityGraph) + 공통 Validator를 통해 소유권 검증을 단일화
        ProductionArtifactDetailEntity artifact = ownershipValidator.validateArtifactOwner(artifactId, userId);

        String s3Key = artifact.getS3Key();
        if (s3Key == null || s3Key.isBlank()) {
            throw new BaseException(ModuleErrorCode.STORAGE_ERROR, null, "Artifact s3Key is not available");
        }

        try {
            return S3PathUtils.extractKey(s3Key);
        } catch (IllegalArgumentException e) {
            throw new BaseException(ModuleErrorCode.VALIDATION_ERROR, "s3Key", "Invalid S3 path: " + s3Key, e);
        }
    }
}

