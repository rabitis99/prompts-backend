package org.example.sharedprompts.module.domain.production.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.model.job.Job;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactRepository;
import org.example.sharedprompts.module.domain.production.service.job.process.recovery.JobRecoveryService;
import org.example.sharedprompts.module.domain.production.service.job.queue.JobQueueService;
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;
import org.example.sharedprompts.module.dto.response.production.JobResponseDto;
import org.example.sharedprompts.module.dto.response.production.ProductionResponseDto;
import org.example.sharedprompts.module.dto.response.production.ProductionResponseDtoMapper;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionResultApplicationService {

    private final ProductionArtifactRepository productionArtifactRepository;
    private final JobQueueService jobQueueService;
    private final JobRecoveryService jobRecoveryService;
    private final ArtifactAccessService artifactAccessService;

    @Transactional(readOnly = true)
    public ProductionResponseDto getProductionResult(Long productionId, Long userId) {
        log.info("Production result requested - productionId: {}, userId: {}", 
                productionId, userId);
        
        ProductionArtifactEntity artifact = productionArtifactRepository
                .findByIdWithDetail(productionId)
                .orElseThrow(() -> new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND));
        
        if (!artifact.getUserId().equals(userId)) {
            throw new BaseException(ModuleErrorCode.PRODUCTION_FORBIDDEN);
        }
        
        return ProductionResponseDtoMapper.toDto(artifact, artifactAccessService);
    }

    @Transactional(readOnly = true)
    public JobResponseDto getJobStatus(String jobId, Long userId) {
        log.info("Job status requested - jobId: {}, userId: {}", jobId, userId);
        
        Job job = jobQueueService.getJob(jobId);
        if (job == null) {
            throw new BaseException(ModuleErrorCode.JOB_NOT_FOUND);
        }
        
        if (!job.getUserId().equals(userId)) {
            throw new BaseException(ModuleErrorCode.JOB_FORBIDDEN);
        }
        
        return JobResponseDto.from(job);
    }

    @Transactional
    public void retryJob(String jobId, Long userId) {
        log.info("Job retry requested - jobId: {}, userId: {}", jobId, userId);
        
        Job job = jobQueueService.getJob(jobId);
        if (job == null) {
            throw new BaseException(ModuleErrorCode.JOB_NOT_FOUND);
        }
        
        if (!job.getUserId().equals(userId)) {
            throw new BaseException(ModuleErrorCode.JOB_FORBIDDEN);
        }
        
        jobRecoveryService.recoverFromParsed(jobId);
    }
}

