package org.example.sharedprompts.module.domain.production.service.production;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.factory.ProductionArtifactEntityFactory;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactRepository;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandler;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandlerRegistry;
import org.example.sharedprompts.module.domain.production.service.artifact.ImageArtifactHandler;
import org.example.sharedprompts.module.domain.production.service.image.ThumbnailService;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.example.sharedprompts.module.domain.production.util.TenantContextValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionArtifactService {

    private final ProductionArtifactRepository productionArtifactRepository;
    private final ArtifactHandlerRegistry artifactHandlerRegistry;
    private final ThumbnailService thumbnailService;

    /**
     * P1-3: S3 I/O를 트랜잭션 밖에서 수행하기 위해 메서드를 분리
     * prepareArtifactData()에서 S3 I/O 수행 후, createArtifactInTx()에서 엔티티 생성 및 저장
     */
    public ProductionArtifactEntity createArtifact(
            JobEntity job,
            String s3Key
    ) {
        // S3 I/O를 트랜잭션 밖에서 수행
        ArtifactHandler handler = prepareArtifactHandler(job, s3Key);
        ImageArtifactHandler.ImageDetailData detailData = null;
        
        if (handler instanceof ImageArtifactHandler imageHandler) {
            // P1-3: S3 I/O를 트랜잭션 밖에서 수행
            detailData = imageHandler.prepareDetailData(s3Key);
        }
        
        // 트랜잭션 안에서 엔티티 생성 및 저장
        return createArtifactInTx(job, s3Key, handler, detailData);
    }

    /**
     * P1-3: S3 I/O를 포함한 artifact handler 준비 (트랜잭션 밖)
     */
    private ArtifactHandler prepareArtifactHandler(JobEntity job, String s3Key) {
        ProductionCommandType commandType;
        try {
            commandType = ProductionCommandType.valueOf(job.getCommandType());
        } catch (IllegalArgumentException e) {
            log.error("Invalid command type: {}", job.getCommandType(), e);
            throw new BaseException(
                    ModuleErrorCode.VALIDATION_ERROR,
                    null,
                    "Invalid command type: " + job.getCommandType(),
                    e);
        }

        // 실제 파일의 contentType을 확인하여 올바른 handler 선택
        String estimatedContentType = ArtifactMetadataHelper.determineContentType(s3Key);
        
        ArtifactType artifactType;
        if (estimatedContentType != null && estimatedContentType.toLowerCase().startsWith("image/")) {
            artifactType = ArtifactType.IMAGE;
        } else if (estimatedContentType != null && estimatedContentType.equals("text/plain")) {
            artifactType = ArtifactType.FILE;
        } else {
            artifactType = ArtifactMetadataHelper.determineArtifactType(commandType);
        }
        
        try {
            return artifactHandlerRegistry.getHandler(artifactType);
        } catch (IllegalArgumentException e) {
            log.error("No ArtifactHandler found for type: {}", artifactType, e);
            throw new BaseException(
                    ModuleErrorCode.VALIDATION_ERROR,
                    null,
                    "No ArtifactHandler found for type: " + artifactType,
                    e);
        }
    }

    /**
     * P1-3: 트랜잭션 안에서 엔티티 생성 및 저장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
    private ProductionArtifactEntity createArtifactInTx(
            JobEntity job,
            String s3Key,
            ArtifactHandler handler,
            ImageArtifactHandler.ImageDetailData detailData
    ) {
        ProductionCommandType commandType;
        try {
            commandType = ProductionCommandType.valueOf(job.getCommandType());
        } catch (IllegalArgumentException e) {
            log.error("Invalid command type: {}", job.getCommandType(), e);
            throw new BaseException(
                    ModuleErrorCode.VALIDATION_ERROR,
                    null,
                    "Invalid command type: " + job.getCommandType(),
                    e);
        }

        String tenantId = TenantContextValidator.requireTenantContext(
                "creating artifact - jobId: " + job.getJobId());

        ProductionArtifactEntity artifact = ProductionArtifactEntityFactory.create(
                job.getId(),
                tenantId,
                job.getUserId(),
                commandType
        );

        // P1-3: S3 I/O는 이미 prepareArtifactHandler()에서 수행됨
        // ImageArtifactHandler의 경우 prepareDetailData()를 먼저 호출하여 S3 I/O를 트랜잭션 밖에서 수행
        ProductionArtifactDetailEntity detail;
        if (handler instanceof ImageArtifactHandler imageHandler && detailData != null) {
            // 트랜잭션 안에서 엔티티 생성 (S3 I/O는 이미 완료됨)
            detail = imageHandler.createDetailFromData(detailData);
        } else {
            // 다른 핸들러는 기존 방식 유지 (향후 개선 필요)
            detail = handler.createDetail(s3Key);
        }
        
        String contentType = detail.getContentType();
        
        // 실제 contentType을 기반으로 artifactType 재조정
        // 참고: image/* 타입 불일치는 경고만 기록 (이미지 핸들러가 다양한 형식 처리 가능)
        // text/plain 타입 불일치는 보정 수행 (텍스트 파일은 FILE 타입으로 명확히 처리 필요)
        if (contentType != null) {
            String lowerContentType = contentType.toLowerCase();
            if (lowerContentType.startsWith("image/")) {
                if (detail.getArtifactType() != ArtifactType.IMAGE) {
                    log.warn("ContentType is image/* but artifactType is not IMAGE - s3Key: {}, artifactType: {}", 
                            s3Key, detail.getArtifactType());
                    // 이미지 핸들러는 다양한 이미지 형식을 처리할 수 있으므로 보정하지 않음
                }
            } else if (lowerContentType.equals("text/plain")) {
                if (detail.getArtifactType() != ArtifactType.FILE) {
                    log.warn("ContentType is text/plain but artifactType is not FILE - s3Key: {}, artifactType: {}. Correcting to FILE.", 
                            s3Key, detail.getArtifactType());
                    ArtifactHandler fileHandler = artifactHandlerRegistry.getHandler(ArtifactType.FILE);
                    detail = fileHandler.createDetail(s3Key);
                }
            }
        }
        
        final ProductionArtifactDetailEntity finalDetail = detail;
        boolean isPrimary = contentType != null && contentType.toLowerCase().startsWith("image/");
        
        if (finalDetail.getArtifact() != null && finalDetail.getArtifact() != artifact) {
            throw new BaseException(
                    ModuleErrorCode.VALIDATION_ERROR,
                    null,
                    "Detail already belongs to another artifact");
        }
        
        artifact.addArtifact(finalDetail);
        
        if (isPrimary) {
            // addArtifact()는 항상 성공하므로 finalDetail은 artifact.getArtifacts()에 포함됨
            // belongsToThis 검사는 항상 true이므로 불필요한 검증 제거
            artifact.markAsPrimary(finalDetail);
        }

        ProductionArtifactEntity saved = productionArtifactRepository.save(artifact);

        ArtifactType actualArtifactType = finalDetail.getArtifactType();
        if (actualArtifactType == ArtifactType.IMAGE && saved.getArtifacts() != null && !saved.getArtifacts().isEmpty()) {
            ProductionArtifactDetailEntity savedDetail = saved.getArtifacts().stream()
                    .filter(ProductionArtifactDetailEntity::isPrimary)
                    .findFirst()
                    .orElse(saved.getArtifacts().get(0));
            Long detailId = savedDetail.getId();
            Long userId = job.getUserId();
            String jobId = job.getJobId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        thumbnailService.generateThumbnailsAsync(detailId, s3Key, tenantId, userId, jobId);
                    } catch (Exception e) {
                        log.warn("Failed to trigger async thumbnail generation - detailId: {}, jobId: {}", detailId, jobId, e);
                    }
                }
            });
        }

        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
    public void deleteArtifact(String artifactId) {
        try {
            Long id = Long.parseLong(artifactId);
            productionArtifactRepository.deleteById(id);
            log.info("Artifact deleted - artifactId: {}", artifactId);
        } catch (NumberFormatException e) {
            log.error("Invalid artifactId format - artifactId: {}", artifactId, e);
            throw new BaseException(
                    ModuleErrorCode.VALIDATION_ERROR,
                    null,
                    "Invalid artifactId format: " + artifactId,
                    e);
        }
    }
}

