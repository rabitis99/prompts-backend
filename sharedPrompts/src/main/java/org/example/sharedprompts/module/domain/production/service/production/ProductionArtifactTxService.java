package org.example.sharedprompts.module.domain.production.service.production;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.entity.factory.ProductionArtifactEntityFactory;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactRepository;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandler;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandlerRegistry;
import org.example.sharedprompts.module.domain.production.service.artifact.ImageArtifactHandler;
import org.example.sharedprompts.module.domain.production.service.image.ThumbnailService;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.util.TenantContextValidator;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Artifact creation in a new transaction so that @Transactional(REQUIRES_NEW) is honored
 * (must be called on another bean for proxy to apply).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionArtifactTxService {

    private final ProductionArtifactRepository productionArtifactRepository;
    private final ArtifactHandlerRegistry artifactHandlerRegistry;
    private final ThumbnailService thumbnailService;

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
    public ProductionArtifactEntity createInNewTransaction(
            JobEntity job,
            String s3Key,
            ArtifactHandler handler,
            ImageArtifactHandler.ImageDetailData detailData,
            ProductionCommandType commandType
    ) {
        String tenantId = TenantContextValidator.requireTenantContext(
                "creating artifact - jobId: " + job.getJobId());

        ProductionArtifactEntity artifact = ProductionArtifactEntityFactory.create(
                job.getId(),
                tenantId,
                job.getUserId(),
                commandType
        );

        ProductionArtifactDetailEntity detail;
        if (handler instanceof ImageArtifactHandler imageHandler && detailData != null) {
            detail = imageHandler.createDetailFromData(detailData);
        } else {
            detail = handler.createDetail(s3Key);
        }

        String contentType = detail.getContentType();

        if (contentType != null) {
            String lowerContentType = contentType.toLowerCase();
            if (lowerContentType.startsWith("image/")) {
                if (detail.getArtifactType() != ArtifactType.IMAGE) {
                    log.warn("ContentType is image/* but artifactType is not IMAGE - s3Key: {}, artifactType: {}",
                            s3Key, detail.getArtifactType());
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
            throw new BaseException(ModuleErrorCode.VALIDATION_ERROR, null,
                    "Detail already belongs to another artifact");
        }

        artifact.addArtifact(finalDetail);

        if (isPrimary) {
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
}
