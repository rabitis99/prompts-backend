package org.example.sharedprompts.module.domain.production.service.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.example.sharedprompts.module.domain.production.service.job.helper.JobUpdateHelper;
import org.example.sharedprompts.module.domain.production.service.production.ProductionArtifactService;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobStateService {

    private static final int MAX_RETRY_COUNT = 3;

    private final JobRepository jobRepository;
    private final JobUpdateHelper jobUpdateHelper;
    private final ProductionArtifactService productionArtifactService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJobPromptVersion(String jobId, String promptVersion) {
        jobUpdateHelper.updateJob(jobId, job -> job.setPromptVersion(promptVersion));
    }

    /**
     * AI 모델 정보 설정
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setModelInfo(String jobId, String modelName, String tokenUsage) {
        jobUpdateHelper.updateJob(jobId, job -> job.setModelInfo(modelName, tokenUsage));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStored(String jobId, String filePath) {
        JobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        // 이미지 생성 시 프롬프트 txt 파일은 artifact로 저장하지 않음
        String estimatedContentType = ArtifactMetadataHelper.determineContentType(filePath);
        ProductionCommandType commandType;
        try {
            commandType = ProductionCommandType.valueOf(job.getCommandType());
        } catch (IllegalArgumentException e) {
            log.error("Invalid command type: {}", job.getCommandType(), e);
            commandType = null;
        }
        
        if (commandType == ProductionCommandType.IMAGE 
                && estimatedContentType != null 
                && estimatedContentType.equals("text/plain")) {
            log.info("Skipping prompt txt file artifact creation for IMAGE command - jobId: {}, filePath: {}", jobId, filePath);
            // 프롬프트 txt 파일은 S3에는 저장되지만 artifact로는 저장하지 않음
            // 기존 artifactId가 있으면 그대로 유지, 없으면 Job은 완료하지 않음 (이미지가 생성될 때까지 대기)
            if (job.getArtifactId() != null && !job.getArtifactId().isBlank()) {
                log.info("Using existing artifactId for prompt txt file - jobId: {}, artifactId: {}", jobId, job.getArtifactId());
            } else {
                log.info("No artifactId for prompt txt file - jobId: {} (waiting for image artifact)", jobId);
            }
            return;
        }

        var artifact = productionArtifactService.createArtifact(job, filePath);
        String artifactId = artifact.getId().toString();

        // Job 완료 처리 (PROCESSING → SUCCEEDED)
        jobUpdateHelper.updateJob(jobId, jobEntity -> jobEntity.complete(artifactId));

        log.info("ProductionArtifact created and job completed - jobId: {}, artifactId: {}, filePath: {}",
                jobId, artifactId, filePath);
    }

    /**
     * Job 완료 처리 (artifact가 이미 생성된 경우)
     * 일반적으로는 markStored()에서 자동으로 호출됨
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String jobId) {
        jobUpdateHelper.updateJob(jobId, jobEntity -> {
            if (jobEntity.getArtifactId() == null || jobEntity.getArtifactId().isBlank()) {
                throw new IllegalStateException("Cannot complete job without artifactId - jobId: " + jobId);
            }
            jobEntity.complete(jobEntity.getArtifactId());
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveJobFailure(String jobId, String errorMessage) {
        try {
            jobUpdateHelper.updateJob(jobId, job -> job.fail(errorMessage));
            log.info("Job failure saved - jobId: {}, error: {}", jobId, errorMessage);
        } catch (IllegalStateException e) {
            log.warn("Could not mark job as failed (already in final state) - jobId: {}", jobId);
        }
    }


    /**
     * Job 재시도 (FAILED → PENDING)
     * 재시도 횟수 증가 후 처음부터 다시 처리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryJob(String jobId) {
        JobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        if (job.getRetryCount() >= MAX_RETRY_COUNT) {
            log.warn("Max retry count reached - jobId: {}, retryCount: {}", jobId, job.getRetryCount());
            throw new IllegalStateException("Max retry count exceeded for job: " + jobId);
        }
        jobUpdateHelper.updateJob(jobId, JobEntity::retry);
        log.info("Job retried - jobId: {}", jobId);
    }

    @Transactional(readOnly = true)
    public JobEntity getJob(String jobId) {
        return jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    }
}
