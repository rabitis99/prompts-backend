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

    @Transactional(propagation = Propagation.REQUIRED)
    public ProductionArtifactEntity createArtifact(
            JobEntity job,
            String s3Key
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

        // 실제 파일의 contentType을 확인하여 올바른 handler 선택
        // 파일명이나 파일 내용을 기반으로 contentType 추정
        String estimatedContentType = ArtifactMetadataHelper.determineContentType(s3Key);
        
        // 실제 파일의 contentType을 기반으로 artifactType 결정
        ArtifactType artifactType;
        if (estimatedContentType != null && estimatedContentType.toLowerCase().startsWith("image/")) {
            // 이미지 파일인 경우
            artifactType = ArtifactType.IMAGE;
        } else if (estimatedContentType != null && estimatedContentType.equals("text/plain")) {
            // 텍스트 파일인 경우 (프롬프트 txt 등)
            artifactType = ArtifactType.FILE;
        } else {
            // 기본값: commandType 기반으로 결정
            artifactType = ArtifactMetadataHelper.determineArtifactType(commandType);
        }
        
        ArtifactHandler handler;
        try {
            handler = artifactHandlerRegistry.getHandler(artifactType);
        } catch (IllegalArgumentException e) {
            log.error("No ArtifactHandler found for type: {}", artifactType, e);
            throw new BaseException(
                    ModuleErrorCode.VALIDATION_ERROR,
                    null,
                    "No ArtifactHandler found for type: " + artifactType,
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

        ProductionArtifactDetailEntity detail = handler.createDetail(s3Key);
        
        // 실제 파일의 contentType을 확인하여 artifactType과 isPrimary 조정
        // handler.createDetail()에서 파일 내용을 읽어서 정확한 contentType을 결정했을 수 있음
        String contentType = detail.getContentType();
        
        // 실제 contentType을 기반으로 artifactType 재조정
        if (contentType != null) {
            String lowerContentType = contentType.toLowerCase();
            if (lowerContentType.startsWith("image/")) {
                // 이미지 파일인 경우: IMAGE 타입, primary=true
                // ImageArtifactHandler가 이미 올바른 artifactType을 설정했을 것
                // 하지만 확실하게 하기 위해 재설정
                if (detail.getArtifactType() != ArtifactType.IMAGE) {
                    // ImageArtifactHandler를 사용했지만 실제로는 이미지가 아닌 경우는 없어야 함
                    log.warn("ContentType is image/* but artifactType is not IMAGE - s3Key: {}, artifactType: {}", 
                            s3Key, detail.getArtifactType());
                }
            } else if (lowerContentType.equals("text/plain")) {
                // 텍스트 파일인 경우: FILE 타입, primary=false
                // FileArtifactHandler를 사용했어야 하지만, 혹시 ImageArtifactHandler를 사용한 경우를 대비
                if (detail.getArtifactType() != ArtifactType.FILE) {
                    log.warn("ContentType is text/plain but artifactType is not FILE - s3Key: {}, artifactType: {}. Correcting to FILE.", 
                            s3Key, detail.getArtifactType());
                    // artifactType을 FILE로 변경하려면 새로운 detail을 생성해야 함
                    // 하지만 detail은 이미 생성되었으므로, FileArtifactHandler를 사용하여 다시 생성
                    ArtifactHandler fileHandler = artifactHandlerRegistry.getHandler(ArtifactType.FILE);
                    detail = fileHandler.createDetail(s3Key);
                }
            }
        }
        
        // 이미지 파일(contentType이 image/*)인 경우만 primary로 설정
        // 프롬프트 txt 파일(text/plain)은 primary=false로 설정
        boolean isPrimary = contentType != null && contentType.toLowerCase().startsWith("image/");
        
        // 검증: detail이 null이 아니고, 다른 artifact에 속해있지 않은지 확인
        if (detail == null) {
            throw new BaseException(
                    ModuleErrorCode.VALIDATION_ERROR,
                    null,
                    "Detail cannot be null");
        }
        if (detail.getArtifact() != null && detail.getArtifact() != artifact) {
            throw new BaseException(
                    ModuleErrorCode.VALIDATION_ERROR,
                    null,
                    "Detail already belongs to another artifact");
        }
        
        artifact.addArtifact(detail);
        
        // Primary 지정은 Aggregate Root에서 통제
        if (isPrimary) {
            // 검증: detail이 이 artifact에 속해있는지 확인
            // addArtifact() 호출 직후이므로 일반적으로 포함되어 있지만, 방어적 프로그래밍을 위해 검증
            boolean belongsToThis = detail.getId() != null
                    ? artifact.getArtifacts().stream().anyMatch(a -> a.getId() != null && a.getId().equals(detail.getId()))
                    : artifact.getArtifacts().contains(detail);
            
            if (!belongsToThis) {
                throw new BaseException(
                        ModuleErrorCode.VALIDATION_ERROR,
                        null,
                        "Detail does not belong to this artifact");
            }
            
            artifact.markAsPrimary(detail);
        }

        ProductionArtifactEntity saved = productionArtifactRepository.save(artifact);

        // detail의 실제 artifactType을 사용 (contentType 기반으로 결정된 값)
        // IMAGE 타입은 contentType이 image/*인 경우에만 설정되므로, 이를 확인하여 thumbnail 생성
        ArtifactType actualArtifactType = detail.getArtifactType();
        if (actualArtifactType == ArtifactType.IMAGE && saved.getArtifacts() != null && !saved.getArtifacts().isEmpty()) {
            // primary artifact 또는 첫 번째 artifact 사용
            ProductionArtifactDetailEntity savedDetail = saved.getArtifacts().stream()
                    .filter(ProductionArtifactDetailEntity::isPrimary)
                    .findFirst()
                    .orElse(saved.getArtifacts().get(0));
            Long detailId = savedDetail.getId();
            Long userId = job.getUserId();
            String jobId = job.getJobId();
            // tenantId를 명시적으로 캡처하여 비동기 스레드에 전달
            // ThreadLocal 기반 TenantContext는 비동기 스레드에 전파되지 않으므로 명시적 전달 필요
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

