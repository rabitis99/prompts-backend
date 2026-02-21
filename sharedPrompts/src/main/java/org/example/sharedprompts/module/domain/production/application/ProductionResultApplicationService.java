package org.example.sharedprompts.module.domain.production.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.model.job.Job;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactRepository;
import org.example.sharedprompts.module.domain.production.service.job.process.JobProcessor;
import org.example.sharedprompts.module.domain.production.service.job.queue.JobQueueService;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandlerRegistry;
import org.example.sharedprompts.module.domain.production.service.production.access.ArtifactOwnershipValidator;
import org.example.sharedprompts.module.dto.response.production.JobResponseDto;
import org.example.sharedprompts.module.dto.response.production.JobResponseDtoMapper;
import org.example.sharedprompts.module.dto.response.production.ProductionResponseDto;
import org.example.sharedprompts.module.dto.response.production.ProductionResponseDtoMapper;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionResultApplicationService {

    private final ProductionArtifactRepository productionArtifactRepository;
    private final JobRepository jobRepository;
    private final JobQueueService jobQueueService;
    private final JobProcessor jobProcessor;
    private final ArtifactHandlerRegistry artifactHandlerRegistry;
    private final ArtifactOwnershipValidator ownershipValidator;

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

        ownershipValidator.validateProductionOwner(artifact, userId);
        
        // Job 정보 조회 (errorMessage, startedAt, completedAt, status를 위해)
        // ProductionArtifactEntity.jobId는 JobEntity.id (PK)를 참조
        // jobId가 null인 레거시 아티팩트를 고려하여 null 체크
        JobEntity jobEntity = Optional.ofNullable(artifact.getJobId())
                .flatMap(jobRepository::findById)
                .orElse(null);
        
        return ProductionResponseDtoMapper.toDto(artifact, jobEntity, artifactHandlerRegistry);
    }

    @Transactional(readOnly = true)
    public JobResponseDto getJobStatus(String jobId, Long userId) {
        log.info("Job status requested - jobId: {}, userId: {}", jobId, userId);
        Job job = getJobOrThrow(jobId, userId);
        return JobResponseDtoMapper.toDto(job);
    }

    @Transactional
    public void retryJob(String jobId, Long userId) {
        log.info("Job retry requested - jobId: {}, userId: {}", jobId, userId);
        Job job = getJobOrThrow(jobId, userId);
        
        // 실패한 Job만 retry 가능
        JobStatus status = job.getStatus();
        if (status == JobStatus.SUCCEEDED) {
            throw new BaseException(ModuleErrorCode.JOB_ALREADY_COMPLETED);
        }
        if (status == JobStatus.PROCESSING) {
            // 처리 중인 Job에 재시도 요청: 아직 완료되지 않은 상태에서 재시도는 불가능
            throw new BaseException(ModuleErrorCode.JOB_INVALID_STATUS);
        }
        if (status == JobStatus.PENDING) {
            // 대기 중인 Job에 재시도 요청: 아직 시작되지 않은 상태에서 재시도는 불가능
            throw new BaseException(ModuleErrorCode.JOB_INVALID_STATUS);
        }
        if (status != JobStatus.FAILED) {
            // FAILED 상태가 아닌 다른 상태 (방어적 프로그래밍)
            throw new BaseException(ModuleErrorCode.JOB_INVALID_STATUS);
        }
        
        // FAILED 상태에서만 재시도 가능
        // JobProcessor.recoverJob()이 retry() 후 처음부터 다시 처리
        jobProcessor.recoverJob(jobId);
    }
}

