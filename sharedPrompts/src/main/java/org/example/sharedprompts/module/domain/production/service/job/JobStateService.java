package org.example.sharedprompts.module.domain.production.service.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.example.sharedprompts.module.domain.production.service.job.helper.JobUpdateHelper;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.JobProcessingException;
import org.example.sharedprompts.module.domain.production.service.production.ProductionArtifactService;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobStateService {

    private static final int MAX_RETRY_COUNT = 3;

    private final JobRepository jobRepository;
    private final JobUpdateHelper jobUpdateHelper;
    private final ProductionArtifactService productionArtifactService;

    /**
     * Job 프롬프트 버전 업데이트
     * JobUpdateHelper에서 트랜잭션을 관리하므로 여기서는 트랜잭션 제거
     */
    public void updateJobPromptVersion(String jobId, String promptVersion) {
        jobUpdateHelper.updateJob(jobId, job -> job.setPromptVersion(promptVersion));
    }

    /**
     * AI 모델 정보 설정
     * JobUpdateHelper에서 트랜잭션을 관리하므로 여기서는 트랜잭션 제거
     */
    public void setModelInfo(String jobId, String modelName, String tokenUsage) {
        jobUpdateHelper.updateJob(jobId, job -> job.setModelInfo(modelName, tokenUsage));
    }

    public void markStored(String jobId, String s3Key) {
        JobEntity job = getJob(jobId);

        // 이미지 생성 시 프롬프트 txt 파일은 artifact로 저장하지 않음
        String estimatedContentType = ArtifactMetadataHelper.determineContentType(s3Key);
        ProductionCommandType commandType;
        try {
            commandType = ProductionCommandType.valueOf(job.getCommandType());
        } catch (IllegalArgumentException e) {
            log.error("Invalid command type: {} - jobId: {}", job.getCommandType(), jobId, e);
            throw new JobProcessingException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    "Invalid command type: " + job.getCommandType() + " for job: " + jobId
            );
        }
        
        if (commandType == ProductionCommandType.IMAGE 
                && estimatedContentType != null 
                && "text/plain".equals(estimatedContentType)) {
            log.info("Skipping prompt txt file artifact creation for IMAGE command - jobId: {}, s3Key: {}", jobId, s3Key);
            if (job.getArtifactId() != null && !job.getArtifactId().isBlank()) {
                log.info("Using existing artifactId for prompt txt file - jobId: {}, artifactId: {}", jobId, job.getArtifactId());
            } else {
                log.info("No artifactId for prompt txt file - jobId: {} (waiting for image artifact)", jobId);
            }
            return;
        }

        // createArtifact는 별도 트랜잭션에서 실행 (S3 I/O 포함 가능)
        var artifact = productionArtifactService.createArtifact(job, s3Key);
        String artifactId = artifact.getId().toString();

        // Job 완료 처리 (PROCESSING → SUCCEEDED) - 별도 트랜잭션
        jobUpdateHelper.updateJob(jobId, jobEntity -> jobEntity.complete(artifactId));

        log.info("ProductionArtifact created and job completed - jobId: {}, artifactId: {}, s3Key: {}",
                jobId, artifactId, s3Key);
    }

    /**
     * Job 완료 처리 (artifact가 이미 생성된 경우)
     * 일반적으로는 markStored()에서 자동으로 호출됨
     * JobUpdateHelper에서 트랜잭션을 관리하므로 여기서는 트랜잭션 제거
     */
    public void markCompleted(String jobId) {
        jobUpdateHelper.updateJob(jobId, jobEntity -> {
            if (jobEntity.getArtifactId() == null || jobEntity.getArtifactId().isBlank()) {
                throw new JobProcessingException(
                        ModuleErrorCode.JOB_INVALID_STATUS,
                        "Cannot complete job without artifactId - jobId: " + jobId
                );
            }
            jobEntity.complete(jobEntity.getArtifactId());
        });
    }

    /**
     * Job 실패 저장
     * JobUpdateHelper에서 트랜잭션을 관리하므로 여기서는 트랜잭션 제거
     */
    public void saveJobFailure(String jobId, String errorMessage) {
        try {
            jobUpdateHelper.updateJob(jobId, job -> job.fail(errorMessage));
            log.info("Job failure saved - jobId: {}, error: {}", jobId, errorMessage);
        } catch (JobProcessingException e) {
            if (e.getErrorCode() == ModuleErrorCode.JOB_INVALID_STATUS || 
                e.getErrorCode() == ModuleErrorCode.JOB_ALREADY_COMPLETED ||
                e.getErrorCode() == ModuleErrorCode.JOB_ALREADY_FAILED) {
                log.warn("Could not mark job as failed (already in final state) - jobId: {}", jobId);
            } else {
                throw e;
            }
        }
    }


    /**
     * Job 재시도 (FAILED → PENDING)
     * 재시도 횟수 증가 후 처음부터 다시 처리
     * JobUpdateHelper에서 트랜잭션을 관리하므로 여기서는 트랜잭션 제거
     */
    public void retryJob(String jobId) {
        // 재시도 횟수 체크를 트랜잭션 내부로 이동하여 TOCTOU 경합 조건 방지
        jobUpdateHelper.updateJob(jobId, job -> {
            if (job.getRetryCount() >= MAX_RETRY_COUNT) {
                log.warn("Max retry count reached - jobId: {}, retryCount: {}", jobId, job.getRetryCount());
                throw new JobProcessingException(
                        ModuleErrorCode.JOB_INVALID_STATUS,
                        "Max retry count exceeded for job: " + jobId
                );
            }
            job.retry();
        });
        log.info("Job retried - jobId: {}", jobId);
    }

    @Transactional(readOnly = true)
    public JobEntity getJob(String jobId) {
        return jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new JobProcessingException(ModuleErrorCode.JOB_NOT_FOUND, "Job not found: " + jobId));
    }
}
