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

    private final JobRepository jobRepository;
    private final JobUpdateHelper jobUpdateHelper;
    private final ProductionArtifactService productionArtifactService;

    public void updateJobPromptVersion(String jobId, String promptVersion) {
        jobUpdateHelper.updateJob(jobId, job -> job.setPromptVersion(promptVersion));
    }

    public void setModelInfo(String jobId, String modelName, String tokenUsage) {
        jobUpdateHelper.updateJob(jobId, job -> job.setModelInfo(modelName, tokenUsage));
    }

    public void markStored(String jobId, String s3Key) {
        JobEntity job = getJob(jobId);

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

        var artifact = productionArtifactService.createArtifact(job, s3Key);
        String artifactId = artifact.getId().toString();

        try {
            jobUpdateHelper.updateJob(jobId, jobEntity -> jobEntity.complete(artifactId));
            log.info("ProductionArtifact created and job completed - jobId: {}, artifactId: {}, s3Key: {}",
                    jobId, artifactId, s3Key);
        } catch (Exception e) {
            log.error("Failed to complete job after artifact creation - jobId: {}, artifactId: {}. Cleaning up orphaned artifact.",
                    jobId, artifactId, e);
            try {
                productionArtifactService.deleteArtifact(artifactId);
                log.info("Orphaned artifact deleted - jobId: {}, artifactId: {}", jobId, artifactId);
            } catch (Exception cleanupException) {
                log.error("Failed to cleanup orphaned artifact - jobId: {}, artifactId: {}", jobId, artifactId, cleanupException);
            }
            throw e;
        }
    }

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
        } catch (IllegalStateException e) {
            log.warn("Could not mark job as failed (already in final state) - jobId: {}, error: {}", jobId, e.getMessage());
        }
    }


    public void retryJob(String jobId) {
        jobUpdateHelper.updateJob(jobId, job -> job.retry());
        log.info("Job retried - jobId: {}", jobId);
    }

    @Transactional(readOnly = true)
    public JobEntity getJob(String jobId) {
        return jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new JobProcessingException(ModuleErrorCode.JOB_NOT_FOUND, "Job not found: " + jobId));
    }
}
