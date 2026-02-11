package org.example.sharedprompts.module.domain.production.service.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.example.sharedprompts.module.domain.production.service.job.helper.JobUpdateHelper;
import org.example.sharedprompts.module.domain.production.service.production.ProductionArtifactService;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobStateService {

    private final JobRepository jobRepository;
    private final JobUpdateHelper jobUpdateHelper;
    private final ProductionArtifactService productionArtifactService;
    private final JobEntityCreationService jobEntityCreationService;
    private final JobLockService jobLockService;

    @Deprecated
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobEntity createJob(
            Long promptId,
            Long userId,
            org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand command,
            String userInput,
            String idempotencyKey
    ) {
        return jobEntityCreationService.createJob(promptId, userId, command, userInput, idempotencyKey);
    }

    @Deprecated
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public java.util.Optional<JobEntity> acquireJobLock(String jobId) {
        return jobLockService.acquireJobLock(jobId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJobPromptVersion(String jobId, String promptVersion) {
        jobUpdateHelper.updateJob(jobId, job -> job.setPromptVersion(promptVersion));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAiCalled(String jobId, String rawResponse, String modelName, String tokenUsage) {
        jobUpdateHelper.updateJob(jobId, job -> job.markAiCalled(rawResponse, modelName, tokenUsage));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markParsed(String jobId, String parsedResponse) {
        jobUpdateHelper.updateJob(jobId, job -> job.markParsed(parsedResponse));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markParseFailed(String jobId, String errorMessage) {
        jobUpdateHelper.updateJob(jobId, job -> job.markParseFailed(errorMessage));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRendered(String jobId, String aiGeneratedContent) {
        jobUpdateHelper.updateJob(jobId, job -> job.markRendered(aiGeneratedContent));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStored(String jobId, String filePath, StorageStrategy storageStrategy) {
        JobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        var artifact = productionArtifactService.createArtifact(job, filePath, storageStrategy);
        String artifactId = artifact.getId().toString();

        jobUpdateHelper.updateJob(jobId, jobEntity -> jobEntity.markStored(artifactId));

        log.info("ProductionArtifact created - jobId: {}, artifactId: {}, filePath: {}",
                jobId, artifactId, filePath);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String jobId) {
        jobUpdateHelper.updateJob(jobId, JobEntity::complete);
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetToParsable(String jobId) {
        jobUpdateHelper.updateJob(jobId, JobEntity::resetToParsable);
    }

    @Transactional(readOnly = true)
    public JobEntity getJob(String jobId) {
        return jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    }
}
