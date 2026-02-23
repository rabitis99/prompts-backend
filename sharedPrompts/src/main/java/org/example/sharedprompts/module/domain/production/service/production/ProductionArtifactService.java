package org.example.sharedprompts.module.domain.production.service.production;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactRepository;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandler;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandlerRegistry;
import org.example.sharedprompts.module.domain.production.service.artifact.ImageArtifactHandler;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionArtifactService {

    private final ProductionArtifactRepository productionArtifactRepository;
    private final ProductionArtifactTxService artifactTxService;
    private final ArtifactHandlerRegistry artifactHandlerRegistry;

    /**
     * P1-3: S3 I/O를 트랜잭션 밖에서 수행하기 위해 메서드를 분리
     * commandType은 한 번만 파싱하여 Tx 서비스에 전달.
     */
    public ProductionArtifactEntity createArtifact(
            JobEntity job,
            String s3Key
    ) {
        ProductionCommandType commandType = parseCommandType(job);
        ArtifactHandler handler = prepareArtifactHandler(job, s3Key, commandType);
        ImageArtifactHandler.ImageDetailData detailData = null;

        if (handler instanceof ImageArtifactHandler imageHandler) {
            detailData = imageHandler.prepareDetailData(s3Key);
        }

        return artifactTxService.createInNewTransaction(job, s3Key, handler, detailData, commandType);
    }

    /**
     * LITERARY 완성 작품: original.txt, preview.html, final.pdf 3개 파일에 대한 아티팩트 생성.
     * 검증은 트랜잭션 진입 전에 수행한다.
     */
    public ProductionArtifactEntity createArtifactForLiterary(
            JobEntity job,
            String originalTxtKey,
            String previewHtmlKey,
            String finalPdfKey
    ) {
        ProductionArtifactTxService.validateLiteraryKeys(originalTxtKey, previewHtmlKey, finalPdfKey);
        return artifactTxService.createInNewTransactionForLiterary(
                job, originalTxtKey, previewHtmlKey, finalPdfKey);
    }

    private ProductionCommandType parseCommandType(JobEntity job) {
        try {
            return ProductionCommandType.valueOf(job.getCommandType());
        } catch (IllegalArgumentException e) {
            log.error("Invalid command type: {}", job.getCommandType(), e);
            throw new BaseException(
                    ModuleErrorCode.VALIDATION_ERROR,
                    null,
                    "Invalid command type: " + job.getCommandType(),
                    e);
        }
    }

    /**
     * P1-3: S3 I/O를 포함한 artifact handler 준비 (트랜잭션 밖)
     */
    private ArtifactHandler prepareArtifactHandler(JobEntity job, String s3Key, ProductionCommandType commandType) {
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

