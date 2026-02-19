package org.example.sharedprompts.module.domain.production.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.model.job.Job;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactRepository;
import org.example.sharedprompts.module.domain.production.service.job.process.recovery.ProcessJobRecoveryService;
import org.example.sharedprompts.module.domain.production.service.job.queue.JobQueueService;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandlerRegistry;
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
    private final ProcessJobRecoveryService processJobRecoveryService;
    private final ArtifactHandlerRegistry artifactHandlerRegistry;

    private Job getJobOrThrow(String jobId, Long userId) {
        Job job = jobQueueService.getJob(jobId);
        if (job == null) {
            throw new BaseException(ModuleErrorCode.JOB_NOT_FOUND);
        }
        if (!job.getUserId().equals(userId)) {
            throw new BaseException(ModuleErrorCode.JOB_FORBIDDEN);
        }
        return job;
    }

    @Transactional(readOnly = true)
    public ProductionResponseDto getProductionResult(Long productionId, Long userId) {
        log.info("Production result requested - productionId: {}, userId: {}", 
                productionId, userId);
        
        ProductionArtifactEntity artifact = productionArtifactRepository
                .findByIdWithArtifacts(productionId)
                .orElseGet(() -> {
                    // 기존 호환성을 위해 detail로도 조회 시도
                    return productionArtifactRepository
                            .findByIdWithDetail(productionId)
                            .orElseThrow(() -> new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND));
                });
        
        if (!artifact.getUserId().equals(userId)) {
            throw new BaseException(ModuleErrorCode.PRODUCTION_FORBIDDEN);
        }
        
        return ProductionResponseDtoMapper.toDto(artifact, artifactHandlerRegistry);
    }

    @Transactional(readOnly = true)
    public JobResponseDto getJobStatus(String jobId, Long userId) {
        log.info("Job status requested - jobId: {}, userId: {}", jobId, userId);
        Job job = getJobOrThrow(jobId, userId);
        return JobResponseDto.from(job);
    }

    @Transactional
    public void retryJob(String jobId, Long userId) {
        log.info("Job retry requested - jobId: {}, userId: {}", jobId, userId);
        Job job = getJobOrThrow(jobId, userId);
        
        // 실패한 Job만 retry 가능
        JobStatus status = job.getStatus();
        if (status == JobStatus.COMPLETED) {
            throw new BaseException(ModuleErrorCode.JOB_ALREADY_COMPLETED);
        }
        if (status != JobStatus.FAILED && status != JobStatus.PARSE_FAILED) {
            throw new BaseException(ModuleErrorCode.JOB_INVALID_STATUS);
        }
        
        processJobRecoveryService.recoverFromParsed(jobId);
    }
}

